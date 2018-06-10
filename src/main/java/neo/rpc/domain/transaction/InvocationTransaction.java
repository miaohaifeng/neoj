package neo.rpc.domain.transaction;

import lombok.Data;
import neo.rpc.domain.Stack;

import java.util.List;

/**
 * @author Miao Haifeng on 2018/5/23
 */
@Data
public class InvocationTransaction {
    private String txid;
    private String vmstate;
    private String gas_consumed;
    private List<Stack> stack;
    private List<Notifications> notifications;
}