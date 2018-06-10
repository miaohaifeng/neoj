package neo.rpc.domain.transaction;

import lombok.Data;

import java.util.List;

/**
 * @author Miao Haifeng on 2018/5/23
 */
@Data
public class State {
    private String type;
    private List<Value> value;
}