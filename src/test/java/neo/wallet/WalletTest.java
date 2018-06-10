package neo.wallet;

import neo.UInt160;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import neo.rpc.domain.Response;
import neo.rpc.domain.wallet.BalanceOfWallet;
import neo.rpc.domain.wallet.RPCError;
import neo.utils.HttpRequestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * @author Miao Haifeng on 2018/6/10
 */
public class WalletTest {

    //把钱包明文转成16进制
    @Test
    public void transNeoWalletAddressToScriptHashTest() {
        String address = "APQ1RGN7xmkEnHQhWYagjuhi3cDD88eZxF";
        String wallet = "0x" + Wallet.toScriptHash(address).toString();
        System.out.println("wallet:" + wallet);
    }

    //获取交易类型
    @Test
    public void getTransferTypeTest() {
        String string160 = "7472616e73666572";
        String transfer = Wallet.hexStringToString(string160);
        System.out.println("transfer:" + transfer);
    }

    //从Neo节点交易日志中获取钱包明文
    @Test
    public void getNeoWalletAddressFromScriptHashTest() {
        String string160 = "2b41aea9d405fef2e809e3c8085221ce944527a7";
        String neoWalletAddress = Wallet.toAddress(UInt160.parseWithoutReverse(string160));
        System.out.println("neoWalletAddress:" + neoWalletAddress);
    }

    //获取余额
    @Test
    public void balanceOfWalletTest() {
        Response<BigDecimal> response = new Response<>();
        String NEO_URL = "http://seed2.neo.org:10332";
        String address = "APQ1RGN7xmkEnHQhWYagjuhi3cDD88eZxF";
        try {
            String wallet = "0x" + Wallet.toScriptHash(address).toString();
            String json = "{\n" +
                    "  \"jsonrpc\": \"2.0\",\n" +
                    "  \"method\": \"invokefunction\",\n" +
                    "  \"params\": [\n" +
                    "    \"0xb951ecbbc5fe37a9c280a76cb0ce0014827294cf\",\n" +
                    "    \"balanceOf\",\n" +
                    "    [\n" +
                    "      {\n" +
                    "        \"type\": \"Hash160\",\n" +
                    "        \"value\": \"" + wallet + "\"\n" +
                    "      }\n" +
                    "    ]\n" +
                    "  ],\n" +
                    "  \"id\": 3\n" +
                    "}";
            String result = HttpRequestUtils.httpInvoke(NEO_URL, json);
            if (StringUtils.isNotEmpty(result) && result.contains("error")) {
                RPCError rpcError = JSON.parseObject(result, new TypeReference<RPCError>() {
                });
                response.setSuccess(false);
                if (null != rpcError && null != rpcError.getError()) {
                    response.setCode(rpcError.getError().getCode() + "");
                    response.setMsg(rpcError.getError().getMessage());
                }

            } else {
                BalanceOfWallet balanceOfWallet = JSON.parseObject(result, new TypeReference<BalanceOfWallet>() {
                });
                String balanceByteArray = balanceOfWallet.getResult().getStack().get(0).getValue();
                long balance = Wallet.byteToLong(Wallet.hexStringToBytes(balanceByteArray));
                BigDecimal divide = new BigDecimal(balance).divide(new BigDecimal(100000000));
                response.setData(divide);
            }
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMsg(e.getMessage());
        }
        System.out.println("jsonString:" + JSON.toJSONString(response));
    }

}