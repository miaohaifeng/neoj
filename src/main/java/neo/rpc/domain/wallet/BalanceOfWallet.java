package neo.rpc.domain.wallet;

import lombok.Data;

/**
 * @author Miao Haifeng on 2018/5/22
 */
@Data
public class BalanceOfWallet {
        private String jsonrpc;
        private int id;
        private Result result;
}