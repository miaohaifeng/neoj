package neo.rpc.domain.wallet;

import lombok.Data;
import neo.rpc.domain.Stack;

import java.util.List;

/**
 * @author Miao Haifeng on 2018/5/22
 */
@Data
public class Result {
    private String script;
    private String state;
    private String gas_consumed;
    private List<Stack> stack;
}