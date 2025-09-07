package com.gym.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * 网关安全配置类
 * 
 * 功能说明：
 * 1. 禁用Spring Security默认安全配置
 * 2. 允许所有请求通过 - 认证由AuthGlobalFilter处理
 * 3. 禁用CSRF保护 - 适用于API网关
 * 
 * @author gym-system
 * @version 1.0
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * 配置安全过滤器链
     * 
     * 网关作为微服务架构的统一入口，需要：
     * 1. 禁用默认的HTTP Basic认证
     * 2. 禁用CSRF保护（API服务不需要）
     * 3. 允许所有请求通过，由自定义的AuthGlobalFilter处理认证
     * 
     * @param http ServerHttpSecurity配置对象
     * @return SecurityWebFilterChain 安全过滤器链
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // 禁用CSRF保护 - API网关不需要CSRF保护
                .csrf().disable()
                
                // 禁用HTTP Basic认证 - 使用JWT认证
                .httpBasic().disable()
                
                // 禁用表单登录 - 使用JWT认证
                .formLogin().disable()
                
                // 禁用logout功能 - JWT是无状态的
                .logout().disable()
                
                // 配置请求授权 - 允许所有请求通过
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()  // 允许所有请求，认证由AuthGlobalFilter处理
                )
                
                .build();
    }
}
