package com.gym.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 认证服务Security配置
 * 
 * 功能说明：
 * 1. 提供密码编码器（用于用户注册时加密密码）
 * 2. 配置简单的Security规则（允许所有请求，因为认证由网关处理）
 * 
 * @author gym-system
 * @version 1.0
 */
@Configuration
public class SecurityConfig {

    /**
     * 密码编码器
     * 用于用户注册时加密密码
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security过滤器链配置
     * 认证服务允许所有请求，因为：
     * 1. 对外请求由网关统一认证
     * 2. 内部服务间调用通过Feign，信任网关
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors().and()
                .csrf().disable()  // 禁用CSRF
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeHttpRequests()
                .anyRequest().permitAll();  // 允许所有请求 - 认证由网关处理

        return http.build();
    }
}
