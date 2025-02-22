package com.gym.result;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class RestResult<T> {
    private Integer code; // Code: 200 indicates success, 0 and other numbers indicate failure

    private String message; // Error message

    private T data; // Data

    private Map map = new HashMap(); // Dynamic data

    public static <T> RestResult<T> success(T object, String msg) {
        RestResult<T> r = new RestResult<T>();
        r.message = msg;
        r.data = object;
        r.code = 200;
        return r;
    }

    public static <T> RestResult<T> error(String msg, Integer code) {
        RestResult r = new RestResult();
        r.message = msg;
        r.code = code;
        return r;
    }

    public RestResult<T> add(String key, Object value) {
        this.map.put(key, value);
        return this;
    }
}
