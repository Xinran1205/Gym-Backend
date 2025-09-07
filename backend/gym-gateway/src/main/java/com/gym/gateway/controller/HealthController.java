package com.gym.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康检查控制器
 * 
 * 功能说明：
 * 1. 提供网关健康状态检查
 * 2. 检查服务发现连接状态
 * 3. 统计已注册的服务实例
 * 4. 提供系统运行时信息
 * 
 * @author gym-system
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private DiscoveryClient discoveryClient;

    /** 网关启动时间 */
    private static final LocalDateTime START_TIME = LocalDateTime.now();

    /**
     * 基础健康检查
     * 
     * @return 健康状态信息
     */
    @GetMapping("/check")
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 基础信息
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            health.put("gateway", "gym-gateway");
            health.put("version", "1.0.0");
            
            // 运行时信息
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> runtimeInfo = new HashMap<>();
            runtimeInfo.put("maxMemory", formatBytes(runtime.maxMemory()));
            runtimeInfo.put("totalMemory", formatBytes(runtime.totalMemory()));
            runtimeInfo.put("freeMemory", formatBytes(runtime.freeMemory()));
            runtimeInfo.put("usedMemory", formatBytes(runtime.totalMemory() - runtime.freeMemory()));
            runtimeInfo.put("availableProcessors", runtime.availableProcessors());
            health.put("runtime", runtimeInfo);
            
            // 启动时间和运行时长
            health.put("startTime", START_TIME.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            long uptimeSeconds = java.time.Duration.between(START_TIME, LocalDateTime.now()).getSeconds();
            health.put("uptime", formatDuration(uptimeSeconds));
            
            log.debug("健康检查完成 - 状态: UP");
            
        } catch (Exception e) {
            log.error("健康检查异常: {}", e.getMessage(), e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return health;
    }

    /**
     * 详细健康检查 (包含服务发现信息)
     * 
     * @return 详细健康状态信息
     */
    @GetMapping("/detail")
    public Map<String, Object> detailHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 基础健康信息
            health.putAll(healthCheck());
            
            // 服务发现信息
            Map<String, Object> discoveryInfo = new HashMap<>();
            try {
                List<String> services = discoveryClient.getServices();
                discoveryInfo.put("status", "UP");
                discoveryInfo.put("registeredServices", services);
                discoveryInfo.put("serviceCount", services.size());
                
                // 统计各服务的实例数量
                Map<String, Integer> serviceInstances = new HashMap<>();
                for (String service : services) {
                    int instanceCount = discoveryClient.getInstances(service).size();
                    serviceInstances.put(service, instanceCount);
                }
                discoveryInfo.put("serviceInstances", serviceInstances);
                
                log.debug("服务发现检查完成 - 已注册服务数: {}", services.size());
                
            } catch (Exception e) {
                log.warn("服务发现检查失败: {}", e.getMessage());
                discoveryInfo.put("status", "DOWN");
                discoveryInfo.put("error", e.getMessage());
            }
            
            health.put("discovery", discoveryInfo);
            
            // JVM信息
            Map<String, Object> jvmInfo = new HashMap<>();
            jvmInfo.put("javaVersion", System.getProperty("java.version"));
            jvmInfo.put("javaVendor", System.getProperty("java.vendor"));
            jvmInfo.put("osName", System.getProperty("os.name"));
            jvmInfo.put("osVersion", System.getProperty("os.version"));
            jvmInfo.put("osArch", System.getProperty("os.arch"));
            health.put("jvm", jvmInfo);
            
        } catch (Exception e) {
            log.error("详细健康检查异常: {}", e.getMessage(), e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return health;
    }

    /**
     * 快速健康检查 (仅返回状态)
     * 
     * @return 简单状态信息
     */
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "pong");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * 服务发现状态检查
     * 
     * @return 服务发现状态信息
     */
    @GetMapping("/discovery")
    public Map<String, Object> discoveryStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            List<String> services = discoveryClient.getServices();
            status.put("status", "UP");
            status.put("services", services);
            status.put("serviceCount", services.size());
            status.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // 获取详细的服务实例信息
            Map<String, Object> serviceDetails = new HashMap<>();
            for (String serviceName : services) {
                try {
                    var instances = discoveryClient.getInstances(serviceName);
                    serviceDetails.put(serviceName, instances.size() + " instances");
                } catch (Exception e) {
                    serviceDetails.put(serviceName, "error: " + e.getMessage());
                }
            }
            status.put("serviceDetails", serviceDetails);
            
        } catch (Exception e) {
            log.error("服务发现状态检查失败: {}", e.getMessage(), e);
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            status.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        return status;
    }

    /**
     * 格式化字节数
     * 
     * @param bytes 字节数
     * @return 格式化后的字符串
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 格式化持续时间
     * 
     * @param seconds 秒数
     * @return 格式化后的字符串
     */
    private String formatDuration(long seconds) {
        long days = seconds / (24 * 3600);
        seconds = seconds % (24 * 3600);
        long hours = seconds / 3600;
        seconds = seconds % 3600;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天 ");
        if (hours > 0) sb.append(hours).append("小时 ");
        if (minutes > 0) sb.append(minutes).append("分钟 ");
        sb.append(seconds).append("秒");
        
        return sb.toString();
    }
}

