package com.gym.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 健身房管理系统网关启动类
 * 
 * 功能说明：
 * 1. 作为微服务架构的统一入口
 * 2. 提供路由转发、负载均衡、熔断降级等功能
 * 3. 集成服务发现，自动发现后端服务实例
 * 4. 支持动态配置刷新
 * 
 * @author gym-system
 * @version 1.0
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class,
    com.baomidou.mybatisplus.autoconfigure.IdentifierGeneratorAutoConfiguration.class
})
@EnableDiscoveryClient  // 启用服务发现功能，与Nacos注册中心集成
@RefreshScope          // 支持配置动态刷新，无需重启即可更新配置
@ComponentScan(
    basePackages = {"com.gym"},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, 
        classes = {com.gym.util.TencentCaptchaUtil.class}
    )
)  // 扫描公共模块的组件，排除腾讯验证码工具类
public class GatewayApplication {

    /**
     * 网关应用程序入口点
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("===============================================");
        System.out.println("🚀 健身房管理系统网关启动成功！");
        System.out.println("📊 监控地址: http://localhost:8888/actuator/health");
        System.out.println("🔗 网关地址: http://localhost:8888");
        System.out.println("===============================================");
    }
}

