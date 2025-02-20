package com.gym.AOP;

import java.lang.annotation.*;

@Target(ElementType.METHOD)        // 作用在方法上
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * timeWindowSeconds: 在多少秒内进行限流统计
     */
    int timeWindowSeconds() default 60;

    /**
     * maxRequests: 在timeWindowSeconds内允许的最大请求数
     */
    int maxRequests() default 10;

    /**
     * 提示信息，可自定义
     */
    String message() default "Too many requests. Please try again later.";
}
