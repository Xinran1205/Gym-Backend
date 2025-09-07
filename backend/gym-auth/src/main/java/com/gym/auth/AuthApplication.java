package com.gym.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 健身房管理系统认证服务启动类
 * 
 * 功能说明：
 * 1. 提供统一的用户认证服务
 * 2. 支持多种认证方式 (JWT、API Token、IP白名单等)
 * 3. 集成服务发现，支持微服务架构
 * 4. 提供用户身份验证和授权功能
 * 
 * 认证策略：
 * - JWT Token认证：用于用户会话管理
 * - API Token认证：用于API调用认证
 * - IP白名单认证：用于内部服务调用
 * 
 * @author gym-system
 * @version 1.0
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
    org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class
})
@EnableDiscoveryClient  // 启用服务发现功能
@EnableFeignClients     // 启用Feign客户端
@ComponentScan(basePackages = {"com.gym"})  // 扫描公共模块的组件
public class AuthApplication {

    /**
     * 认证服务应用程序入口点
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
        System.out.println("===============================================");
        System.out.println("🔐 健身房管理系统认证服务启动成功！");
        System.out.println("📊 监控地址: http://localhost:8081/actuator/health");
        System.out.println("🔗 服务地址: http://localhost:8081");
        System.out.println("📚 API文档: http://localhost:8081/doc.html");
        System.out.println("===============================================");
    }
}

