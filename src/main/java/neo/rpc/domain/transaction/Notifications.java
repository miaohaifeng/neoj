package neo.rpc.domain.transaction;

import lombok.Data;

/**
 * @author Miao Haifeng on 2018/5/23
 */
@Data
public class Notifications {
    private String contract;
    private State state;
}