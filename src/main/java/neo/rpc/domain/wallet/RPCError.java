package neo.rpc.domain.wallet;

import lombok.Data;

/**
 * @author Miao Haifeng on 2018/5/22
 */
@Data
public class RPCError {
    private String jsonrpc;
    private String id;
    private Error error;
}