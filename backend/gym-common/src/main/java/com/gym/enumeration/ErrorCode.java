package com.gym.enumeration;

public enum ErrorCode {

    SUCCESS(200, "Success"),
    BAD_REQUEST(400, "Bad Request"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    TOO_MANY_REQUESTS(429, "Too Many Requests"),
    UNAUTHORIZED(401, "Unauthorized"),
    //用户待审核的教练申请超过5个
    TRAINER_REQUEST_LIMIT(1001, "Trainer request limit exceeded."),
    // 禁止操作
    FORBIDDEN(1002, "Forbidden"),
    // You can add more custom error codes here if needed
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
