package neo.rpc.domain;

import lombok.Data;

/**
 * @author Miao Haifeng on 2018/5/22
 */
@Data
public class Response<T> {
    private int status;//1:表示成功，-1：表示失败
    private boolean success = true;
    private String code;
    private String msg;
    private T data;


}