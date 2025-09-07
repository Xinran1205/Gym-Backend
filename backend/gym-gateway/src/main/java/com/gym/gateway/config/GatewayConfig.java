package com.gym.gateway.config;

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
                
                // 设置跨域响应头 - 修复CORS配置冲突
                String origin = request.getHeaders().getOrigin();
                if (origin != null) {
                    headers.add("Access-Control-Allow-Origin", origin);  // 动态设置来源
                } else {
                    headers.add("Access-Control-Allow-Origin", "http://localhost:5173");  // 默认前端地址
                }
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

    // 注意：AuthGlobalFilter、LoggingGlobalFilter、SecurityHeadersGlobalFilter 
    // 已经通过 @Component 注解自动注册，无需在此重复定义
}

