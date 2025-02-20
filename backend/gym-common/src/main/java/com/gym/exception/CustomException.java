package com.gym.exception;

import com.gym.enumeration.ErrorCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        // Pass the default message of the ErrorCode to the superclass
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * If you want to specify a custom message overriding the enum's default:
     */
    public CustomException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
