package com.gym.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 安全头全局过滤器
 * 
 * 功能说明：
 * 1. 自动注入HTTP安全响应头
 * 2. 防止常见的Web安全攻击
 * 3. 提升应用整体安全性
 * 4. 符合安全最佳实践
 * 
 * 安全头说明：
 * - X-Content-Type-Options: 防止MIME类型嗅探攻击
 * - X-Frame-Options: 防止点击劫持攻击
 * - X-XSS-Protection: 启用XSS过滤器
 * - Strict-Transport-Security: 强制使用HTTPS
 * - Content-Security-Policy: 内容安全策略
 * - Referrer-Policy: 控制引用信息泄露
 * - Cache-Control: 控制缓存策略
 * 
 * @author gym-system
 * @version 1.0
 */
@Slf4j
@Component
public class SecurityHeadersGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 过滤器执行逻辑
     * 
     * @param exchange 服务器交换对象
     * @param chain 过滤器链
     * @return Mono<Void> 异步结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();
            
            // 添加安全响应头
            addSecurityHeaders(headers);
            
            log.debug("已为响应添加安全头: {}", exchange.getRequest().getPath().value());
        }));
    }

    /**
     * 添加安全响应头
     * 
     * @param headers HTTP响应头
     */
    private void addSecurityHeaders(HttpHeaders headers) {
        
        // 1. X-Content-Type-Options: 防止MIME类型嗅探攻击
        // 告诉浏览器不要尝试猜测内容类型，严格按照Content-Type执行
        if (!headers.containsKey("X-Content-Type-Options")) {
            headers.add("X-Content-Type-Options", "nosniff");
        }

        // 2. X-Frame-Options: 防止点击劫持攻击
        // 防止页面被嵌入到其他站点的frame或iframe中
        if (!headers.containsKey("X-Frame-Options")) {
            headers.add("X-Frame-Options", "DENY");
        }

        // 3. X-XSS-Protection: 启用浏览器XSS过滤器
        // 启用浏览器内置的XSS防护机制
        if (!headers.containsKey("X-XSS-Protection")) {
            headers.add("X-XSS-Protection", "1; mode=block");
        }

        // 4. Strict-Transport-Security: 强制使用HTTPS
        // 告诉浏览器在指定时间内只能通过HTTPS访问网站
        if (!headers.containsKey("Strict-Transport-Security")) {
            headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        }

        // 5. Content-Security-Policy: 内容安全策略
        // 防止XSS攻击，控制页面可以加载的资源
        if (!headers.containsKey("Content-Security-Policy")) {
            // 开发环境使用相对宽松的策略，生产环境建议更严格
            String csp = "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://apis.google.com; " +
                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                        "font-src 'self' https://fonts.gstatic.com; " +
                        "img-src 'self' data: https:; " +
                        "connect-src 'self' https://api.github.com https://accounts.google.com; " +
                        "frame-src 'none'; " +
                        "object-src 'none'";
            headers.add("Content-Security-Policy", csp);
        }

        // 6. Referrer-Policy: 控制引用信息
        // 控制在请求中发送多少引用信息
        if (!headers.containsKey("Referrer-Policy")) {
            headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
        }

        // 7. X-Permitted-Cross-Domain-Policies: 控制跨域策略文件
        // 限制Flash和PDF等插件的跨域策略
        if (!headers.containsKey("X-Permitted-Cross-Domain-Policies")) {
            headers.add("X-Permitted-Cross-Domain-Policies", "none");
        }

        // 8. Feature-Policy: 功能策略 (已被Permissions-Policy取代，但保持兼容)
        // 控制浏览器功能的使用
        if (!headers.containsKey("Feature-Policy")) {
            String featurePolicy = "geolocation 'self'; " +
                                 "microphone 'none'; " +
                                 "camera 'none'; " +
                                 "payment 'none'; " +
                                 "usb 'none'";
            headers.add("Feature-Policy", featurePolicy);
        }

        // 9. Permissions-Policy: 新的功能权限策略
        // 更现代的方式控制浏览器功能
        if (!headers.containsKey("Permissions-Policy")) {
            String permissionsPolicy = "geolocation=(), " +
                                     "microphone=(), " +
                                     "camera=(), " +
                                     "payment=(), " +
                                     "usb=()";
            headers.add("Permissions-Policy", permissionsPolicy);
        }

        // 10. Cache-Control: 缓存控制
        // 为敏感数据设置不缓存策略
        String path = headers.getFirst("X-Request-Path");
        if (path != null && (path.contains("/auth/") || path.contains("/admin/"))) {
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
        }

        // 11. Server: 隐藏服务器信息
        // 避免暴露服务器技术栈信息
        headers.remove("Server");
        
        // 12. X-Powered-By: 移除技术栈标识
        // 避免暴露框架信息
        headers.remove("X-Powered-By");

        log.debug("安全头添加完成，共添加了 {} 个安全相关的响应头", 
                 countSecurityHeaders(headers));
    }

    /**
     * 统计安全头数量
     * 
     * @param headers HTTP头
     * @return 安全头数量
     */
    private int countSecurityHeaders(HttpHeaders headers) {
        String[] securityHeaders = {
            "X-Content-Type-Options", "X-Frame-Options", "X-XSS-Protection",
            "Strict-Transport-Security", "Content-Security-Policy", "Referrer-Policy",
            "X-Permitted-Cross-Domain-Policies", "Feature-Policy", "Permissions-Policy"
        };
        
        int count = 0;
        for (String header : securityHeaders) {
            if (headers.containsKey(header)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 过滤器执行顺序
     * 
     * 在响应返回前执行，优先级较低
     * 
     * @return 执行顺序
     */
    @Override
    public int getOrder() {
        return -50; // 在认证过滤器之后，但在业务过滤器之前执行
    }
}

