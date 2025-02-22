package com.gym.exception;

import com.gym.enumeration.ErrorCode;
import com.gym.result.RestResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle all CustomExceptions thrown from controllers or services
     */
    @ExceptionHandler(CustomException.class)
    public RestResult<?> handleCustomException(CustomException ex) {
        log.error("CustomException caught: {}", ex.getMessage(), ex);
        ErrorCode errorCode = ex.getErrorCode();
        // You could choose to use either the default message from errorCode or ex.getMessage()
        // If you used the constructor with a custom message, ex.getMessage() might differ from errorCode.getMessage()
        return RestResult.error(ex.getMessage(), errorCode.getCode());
    }

    /**
     * Handle all unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public RestResult<?> handleException(Exception ex) {
        log.error("Unknown exception: {}", ex.getMessage(), ex);
        // Return a generic internal server error
        return RestResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                ErrorCode.INTERNAL_SERVER_ERROR.getCode());
    }

    // ====== 新增：处理校验失败异常 ======
    // 通常使用 @Valid 注解校验 DTO 时发生错误,用来抛异常。前端传来的数据是否符合要求，不符合就会抛出异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RestResult<?> handleValidationException(MethodArgumentNotValidException ex) {
        // 从异常对象中拿到所有字段错误
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();

        // 将多个错误拼成一个字符串，或者你也可以返回一个map
        StringBuilder errorMsg = new StringBuilder("Validation failed: ");
        for (FieldError fieldError : fieldErrors) {
            errorMsg
                    .append("[")
                    .append(fieldError.getField())
                    .append(": ")
                    .append(fieldError.getDefaultMessage()) // 这里就是注解中设置的 message
                    .append("] ");
        }

        log.error("Validation error: {}", errorMsg.toString());

        // 这里你可以返回一个 400 状态码，或自定义 ErrorCode
        // 下面演示返回400，以及将拼好的报错信息返回给前端
        return RestResult.error(errorMsg.toString(), 400);
    }
}
