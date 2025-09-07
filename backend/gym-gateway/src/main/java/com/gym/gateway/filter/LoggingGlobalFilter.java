package com.gym.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 日志记录全局过滤器
 * 
 * 功能说明：
 * 1. 记录请求和响应信息
 * 2. 生成链路追踪ID
 * 3. 计算请求处理时间
 * 4. 监控接口性能
 * 
 * @author gym-system
 * @version 1.0
 */
@Slf4j
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    /** 时间格式化器 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** 链路追踪ID请求头名称 */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 过滤器执行逻辑
     * 
     * @param exchange 服务器交换对象
     * @param chain 过滤器链
     * @return Mono<Void> 异步结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        // 生成链路追踪ID
        String traceId = generateTraceId();
        
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        String startTimeStr = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        
        // 提取请求信息
        String method = request.getMethod().toString();
        String path = request.getPath().value();
        String query = request.getURI().getQuery();
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String clientIp = getClientIp(request);
        String referer = request.getHeaders().getFirst("Referer");
        
        // 构建完整URL
        String fullUrl = path + (query != null ? "?" + query : "");
        
        // 记录请求开始日志
        log.info("🚀 请求开始 [{}] - {} {} - 客户端IP: {} - 时间: {} - UserAgent: {}", 
                traceId, method, fullUrl, clientIp, startTimeStr, userAgent);
        
        // 记录请求头信息 (仅在DEBUG级别)
        if (log.isDebugEnabled()) {
            log.debug("📥 请求头 [{}] - {}", traceId, request.getHeaders().toSingleValueMap());
            if (referer != null) {
                log.debug("🔗 来源页面 [{}] - {}", traceId, referer);
            }
        }
        
        // 将链路追踪ID添加到请求头和响应头
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(TRACE_ID_HEADER, traceId)
                .build();
        
        response.getHeaders().add(TRACE_ID_HEADER, traceId);
        
        // 创建新的交换对象
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();
        
        // 继续执行过滤器链，并在完成后记录响应信息
        return chain.filter(mutatedExchange)
                .doOnSuccess(aVoid -> {
                    // 请求成功完成
                    logRequestCompletion(traceId, method, fullUrl, clientIp, 
                                       startTime, response.getStatusCode().value(), true);
                })
                .doOnError(throwable -> {
                    // 请求处理出错
                    log.error("❌ 请求异常 [{}] - {} {} - 客户端IP: {} - 错误: {}", 
                            traceId, method, fullUrl, clientIp, throwable.getMessage());
                    logRequestCompletion(traceId, method, fullUrl, clientIp, 
                                       startTime, 500, false);
                })
                .doFinally(signalType -> {
                    // 无论成功还是失败都会执行
                    log.debug("🏁 请求结束 [{}] - 信号类型: {}", traceId, signalType);
                });
    }

    /**
     * 记录请求完成信息
     * 
     * @param traceId 链路追踪ID
     * @param method HTTP方法
     * @param fullUrl 完整URL
     * @param clientIp 客户端IP
     * @param startTime 开始时间
     * @param statusCode 响应状态码
     * @param success 是否成功
     */
    private void logRequestCompletion(String traceId, String method, String fullUrl, 
                                    String clientIp, long startTime, int statusCode, boolean success) {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        String endTimeStr = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        
        // 根据响应时间和状态码选择不同的日志级别
        String statusEmoji = getStatusEmoji(statusCode);
        String performanceEmoji = getPerformanceEmoji(duration);
        
        if (success && statusCode < 400) {
            log.info("✅ 请求完成 [{}] - {} {} - 状态码: {}{} - 耗时: {}ms{} - 客户端IP: {} - 时间: {}", 
                    traceId, method, fullUrl, statusCode, statusEmoji, duration, performanceEmoji, clientIp, endTimeStr);
        } else {
            log.warn("⚠️ 请求异常 [{}] - {} {} - 状态码: {}{} - 耗时: {}ms{} - 客户端IP: {} - 时间: {}", 
                    traceId, method, fullUrl, statusCode, statusEmoji, duration, performanceEmoji, clientIp, endTimeStr);
        }
        
        // 性能监控：记录慢请求
        if (duration > 2000) { // 超过2秒的请求
            log.warn("🐌 慢请求告警 [{}] - {} {} - 耗时: {}ms - 客户端IP: {}", 
                    traceId, method, fullUrl, duration, clientIp);
        }
    }

    /**
     * 生成链路追踪ID
     * 
     * @return 链路追踪ID
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 获取客户端真实IP地址
     * 
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIp(ServerHttpRequest request) {
        // 尝试从各种代理头中获取真实IP
        String[] headers = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", 
            "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeaders().getFirst(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 如果有多个IP，取第一个
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        // 如果都没有，返回远程地址
        return request.getRemoteAddress() != null ? 
               request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    /**
     * 根据HTTP状态码获取对应的表情符号
     * 
     * @param statusCode HTTP状态码
     * @return 表情符号
     */
    private String getStatusEmoji(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return " ✅"; // 成功
        } else if (statusCode >= 300 && statusCode < 400) {
            return " 🔄"; // 重定向
        } else if (statusCode >= 400 && statusCode < 500) {
            return " ❌"; // 客户端错误
        } else if (statusCode >= 500) {
            return " 💥"; // 服务器错误
        }
        return "";
    }

    /**
     * 根据响应时间获取性能表情符号
     * 
     * @param duration 响应时间(毫秒)
     * @return 性能表情符号
     */
    private String getPerformanceEmoji(long duration) {
        if (duration < 100) {
            return " ⚡"; // 极快
        } else if (duration < 500) {
            return " 🚀"; // 快
        } else if (duration < 1000) {
            return " 🏃"; // 正常
        } else if (duration < 2000) {
            return " 🚶"; // 较慢
        } else {
            return " 🐌"; // 很慢
        }
    }

    /**
     * 过滤器执行顺序
     * 
     * @return 执行顺序 (数值越小，优先级越高)
     */
    @Override
    public int getOrder() {
        return -200; // 最高优先级，确保在所有其他过滤器之前执行
    }
}

