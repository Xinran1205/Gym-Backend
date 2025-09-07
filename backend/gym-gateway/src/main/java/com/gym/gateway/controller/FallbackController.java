package com.gym.gateway.controller;

import com.gym.result.RestResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 熔断降级控制器
 * 
 * 功能说明：
 * 1. 提供服务熔断时的降级响应
 * 2. 为不同服务提供个性化降级处理
 * 3. 维护系统整体可用性
 * 4. 提供友好的错误提示
 * 
 * 降级策略：
 * - 认证服务降级：返回认证不可用提示
 * - 业务服务降级：返回服务繁忙提示
 * - 管理服务降级：返回管理功能暂停提示
 * 
 * @author gym-system
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * 认证服务降级处理
     * 
     * 当认证服务不可用时的降级响应
     * 
     * @return 降级响应结果
     */
    @GetMapping("/auth")
    public RestResult<Object> authFallback() {
        log.warn("🔥 认证服务触发熔断降级");
        
        return RestResult.error(
            "认证服务暂时不可用，请稍后重试。如果问题持续存在，请联系系统管理员。", 
            503
        ).add("fallback", true)
         .add("service", "gym-auth")
         .add("suggestion", "请检查网络连接或稍后重试");
    }

    /**
     * 业务服务降级处理
     * 
     * 当主要业务服务不可用时的降级响应
     * 
     * @return 降级响应结果
     */
    @GetMapping("/server")
    public RestResult<Object> serverFallback() {
        log.warn("🔥 业务服务触发熔断降级");
        
        return RestResult.error(
            "业务服务当前繁忙，请稍后重试。我们正在努力恢复服务。", 
            503
        ).add("fallback", true)
         .add("service", "gym-server")
         .add("suggestion", "请稍后重试或使用离线功能");
    }

    /**
     * 管理服务降级处理
     * 
     * 当管理功能不可用时的降级响应
     * 
     * @return 降级响应结果
     */
    @GetMapping("/admin")
    public RestResult<Object> adminFallback() {
        log.warn("🔥 管理服务触发熔断降级");
        
        return RestResult.error(
            "管理功能暂时不可用，请稍后重试。如需紧急处理，请联系技术支持。", 
            503
        ).add("fallback", true)
         .add("service", "gym-admin")
         .add("suggestion", "请联系技术支持或稍后重试")
         .add("contact", "admin@gym-system.com");
    }

    /**
     * 通用降级处理
     * 
     * 当没有特定降级处理时的通用响应
     * 
     * @return 降级响应结果
     */
    @GetMapping("/default")
    public RestResult<Object> defaultFallback() {
        log.warn("🔥 服务触发通用熔断降级");
        
        return RestResult.error(
            "服务暂时不可用，请稍后重试。", 
            503
        ).add("fallback", true)
         .add("service", "unknown")
         .add("timestamp", System.currentTimeMillis());
    }

    /**
     * 健康检查降级处理
     * 
     * 当健康检查服务不可用时的降级响应
     * 
     * @return 降级响应结果
     */
    @GetMapping("/health")
    public RestResult<Object> healthFallback() {
        log.warn("🔥 健康检查服务触发熔断降级");
        
        return RestResult.error(
            "健康检查服务不可用", 
            503
        ).add("fallback", true)
         .add("service", "health-check")
         .add("status", "degraded");
    }
}

