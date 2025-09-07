package com.gym.gateway.config;

import com.gym.gateway.filter.CanaryGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关路由配置类
 * 
 * 功能说明：
 * 1. 定义服务路由规则
 * 2. 配置负载均衡策略
 * 3. 设置熔断降级
 * 4. 支持灰度发布
 * 
 * @author gym-system
 * @version 1.0
 */
@Configuration
public class RouteConfig {

    /**
     * 自定义路由配置
     * 
     * 路由规则说明：
     * 1. /auth/** -> gym-auth 认证服务
     * 2. /api/** -> gym-server 业务服务
     * 3. /admin/** -> gym-server 管理功能
     * 
     * @param builder 路由构建器
     * @param canaryFilterFactory 灰度发布过滤器工厂
     * @return RouteLocator 路由定位器
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, 
                                          CanaryGatewayFilterFactory canaryFilterFactory) {
        return builder.routes()
                // 认证服务路由
                .route("gym-auth-route", r -> r
                        .path("/auth/**")  // 匹配 /auth/ 开头的所有请求
                        .filters(f -> f
                                .stripPrefix(1)  // 去掉路径前缀 /auth
                                .circuitBreaker(config -> config  // 配置熔断器
                                        .setName("gym-auth-cb")
                                        .setFallbackUri("forward:/fallback/auth"))
                                .filter(canaryFilterFactory.apply(new CanaryGatewayFilterFactory.Config()))  // 灰度发布
                                .retry(retryConfig -> retryConfig  // 重试配置
                                        .setRetries(3)
                                        .setBackoff(java.time.Duration.ofSeconds(1), 
                                                   java.time.Duration.ofSeconds(5), 2, false)))
                        .uri("lb://gym-auth"))  // 负载均衡到 gym-auth 服务

                // 业务服务路由 - API接口
                .route("gym-server-api-route", r -> r
                        .path("/api/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .circuitBreaker(config -> config
                                        .setName("gym-server-cb")
                                        .setFallbackUri("forward:/fallback/server"))
                                .filter(canaryFilterFactory.apply(new CanaryGatewayFilterFactory.Config()))
                                .retry(retryConfig -> retryConfig
                                        .setRetries(2)
                                        .setMethods(org.springframework.http.HttpMethod.GET, 
                                                   org.springframework.http.HttpMethod.POST)
                                        .setBackoff(java.time.Duration.ofMillis(500), 
                                                   java.time.Duration.ofSeconds(2), 2, false)))
                        .uri("lb://gym-server"))

                // 业务服务路由 - 管理接口
                .route("gym-server-admin-route", r -> r
                        .path("/admin/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .circuitBreaker(config -> config
                                        .setName("gym-server-admin-cb")
                                        .setFallbackUri("forward:/fallback/admin"))
                                .filter(canaryFilterFactory.apply(new CanaryGatewayFilterFactory.Config())))
                        .uri("lb://gym-server"))

                // 健康检查路由 (直接访问，不需要认证)
                .route("health-check-route", r -> r
                        .path("/health/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://gym-server"))

                // 静态资源路由 (文档、图片等)
                .route("static-resource-route", r -> r
                        .path("/static/**", "/doc.html", "/swagger-resources/**", "/webjars/**", "/v2/api-docs")
                        .uri("lb://gym-server"))

                .build();
    }
}

