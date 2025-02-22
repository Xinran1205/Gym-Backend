package com.gym.AOP;

import com.gym.exception.CustomException;
import com.gym.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.gym.enumeration.ErrorCode;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private HttpServletRequest request;

    /**
     * 配置切点：
     * 拦截所有被 @RateLimit 注解标记的方法
     */
    @Pointcut("@annotation(com.gym.AOP.RateLimit)")
    public void rateLimitPointcut() {}

    /**
     * 环绕通知，执行具体的限流逻辑
     */
    @Around("rateLimitPointcut() && @annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {

        // 1. 构造限流Key：可用 客户端IP + methodSignature
        String clientIp = IpUtil.getClientIp(request);
        String methodName = joinPoint.getSignature().toShortString();
        String redisKey = "RATE_LIMIT:" + methodName + ":" + clientIp;

        int timeWindowSeconds = rateLimit.timeWindowSeconds();
        int maxRequests = rateLimit.maxRequests();
        String message = rateLimit.message();

        // 2. 使用 Redis incr 进行计数
        Long currentCount = redisTemplate.opsForValue().increment(redisKey, 1);
        if (currentCount != null) {
            // 如果是第一次计数，设置过期时间
            if (currentCount == 1) {
                redisTemplate.expire(redisKey, timeWindowSeconds, TimeUnit.SECONDS);
            }

            // 如果超出限制，抛出异常
            if (currentCount > maxRequests) {
                log.warn("Rate limit triggered, key={}, count={}, limit={}", redisKey, currentCount, maxRequests);
                throw new CustomException(ErrorCode.TOO_MANY_REQUESTS, message);
            }
        }

        // 3. 继续执行被拦截的方法
        return joinPoint.proceed();
    }
}

//    private String getClientIp(HttpServletRequest request) {
//        String ip = request.getHeader("X-Forwarded-For");
//        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getHeader("Proxy-Client-IP");
//        }
//        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getHeader("WL-Proxy-Client-IP");
//        }
//        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getRemoteAddr();
//        }
//        // 如果有多个IP，取第一个
//        return ip.split(",")[0].trim();
//    }