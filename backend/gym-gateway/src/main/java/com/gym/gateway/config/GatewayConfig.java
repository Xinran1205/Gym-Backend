package com.gym.gateway.config;

import com.gym.gateway.filter.AuthGlobalFilter;
import com.gym.gateway.filter.LoggingGlobalFilter;
import com.gym.gateway.filter.SecurityHeadersGlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 网关全局配置类
 * 
 * 功能说明：
 * 1. 配置跨域处理 (CORS)
 * 2. 注册全局过滤器
 * 3. 配置安全策略
 * 
 * @author gym-system
 * @version 1.0
 */
@Configuration
public class GatewayConfig {

    /**
     * 跨域处理配置
     * 
     * 解决前端跨域访问问题，支持：
     * - 允许所有来源 (生产环境建议限制具体域名)
     * - 支持常用HTTP方法
     * - 允许携带认证信息
     * - 设置预检请求缓存时间
     * 
     * @return WebFilter 跨域处理过滤器
     */
    @Bean
    public WebFilter corsFilter() {
        return (ServerWebExchange ctx, WebFilterChain chain) -> {
            ServerHttpRequest request = ctx.getRequest();
            
            // 检查是否为跨域请求
            if (CorsUtils.isCorsRequest(request)) {
                ServerHttpResponse response = ctx.getResponse();
                HttpHeaders headers = response.getHeaders();
                
                // 设置跨域响应头
                headers.add("Access-Control-Allow-Origin", "*");  // 允许所有来源
                headers.add("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS");
                headers.add("Access-Control-Max-Age", "18000");   // 预检缓存时长30分钟
                headers.add("Access-Control-Allow-Headers", "*"); // 允许所有请求头
                headers.add("Access-Control-Expose-Headers", "*"); // 暴露所有响应头
                headers.add("Access-Control-Allow-Credentials", "true"); // 允许携带认证信息
                
                // 处理预检请求 (OPTIONS)
                if (request.getMethod() == HttpMethod.OPTIONS) {
                    response.setStatusCode(HttpStatus.OK);
                    return Mono.empty(); // 直接返回成功，不继续执行后续逻辑
                }
            }
            
            // 继续执行过滤器链
            return chain.filter(ctx);
        };
    }

    /**
     * 注册认证全局过滤器
     * 
     * 功能：
     * - JWT token验证
     * - 用户身份识别
     * - 权限检查
     * 
     * @return AuthGlobalFilter 认证过滤器实例
     */
    @Bean
    public AuthGlobalFilter authGlobalFilter() {
        return new AuthGlobalFilter();
    }

    /**
     * 注册日志记录全局过滤器
     * 
     * 功能：
     * - 请求响应日志记录
     * - 性能监控
     * - 链路追踪
     * 
     * @return LoggingGlobalFilter 日志过滤器实例
     */
    @Bean
    public LoggingGlobalFilter loggingGlobalFilter() {
        return new LoggingGlobalFilter();
    }

    /**
     * 注册安全头全局过滤器
     * 
     * 功能：
     * - 注入HTTP安全响应头
     * - 防止常见Web攻击
     * - 提升应用安全性
     * 
     * @return SecurityHeadersGlobalFilter 安全头过滤器实例
     */
    @Bean
    public SecurityHeadersGlobalFilter securityHeadersGlobalFilter() {
        return new SecurityHeadersGlobalFilter();
    }
}

