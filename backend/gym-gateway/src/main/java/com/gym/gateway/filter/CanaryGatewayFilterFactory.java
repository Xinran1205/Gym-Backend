package com.gym.gateway.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * 灰度发布网关过滤器工厂
 * 
 * 功能说明：
 * 1. 支持基于请求头的灰度路由
 * 2. 支持基于IP白名单的灰度路由
 * 3. 动态切换到灰度环境
 * 4. 支持外部URL灰度发布
 * 
 * 灰度策略：
 * - Header策略：检查特定请求头值
 * - IP策略：检查客户端IP是否在白名单中
 * - 版本策略：根据版本号路由到不同服务
 * 
 * @author gym-system
 * @version 1.0
 */
@Slf4j
@Component
public class CanaryGatewayFilterFactory extends AbstractGatewayFilterFactory<CanaryGatewayFilterFactory.Config> {

    /** 是否开启灰度发布 */
    @Value("${canary.enabled:false}")
    private boolean canaryEnabled;

    /** 灰度标识请求头名称 */
    @Value("${canary.header-key:X-Canary-Flag}")
    private String canaryHeaderKey;

    /** 灰度标识值列表 */
    @Value("${canary.header-values:canary,test,beta}")
    private String canaryHeaderValues;

    /** 灰度IP白名单 */
    @Value("${canary.ip-whitelist:127.0.0.1,::1}")
    private String canaryIpWhitelist;

    /** 是否使用外部URL */
    @Value("${canary.external-url.enabled:false}")
    private boolean externalUrlEnabled;

    /** 外部灰度URL */
    @Value("${canary.external-url.target:}")
    private String externalUrlTarget;

    public CanaryGatewayFilterFactory() {
        super(Config.class);
    }

    /**
     * 创建灰度发布过滤器
     * 
     * @param config 过滤器配置
     * @return GatewayFilter 网关过滤器
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 如果未启用灰度发布，直接放行
            if (!canaryEnabled) {
                log.debug("灰度发布未启用，请求正常路由");
                return chain.filter(exchange);
            }

            ServerHttpRequest request = exchange.getRequest();
            Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            
            // 检查路由是否支持负载均衡
            if (route == null || !route.getUri().toString().startsWith("lb://")) {
                log.debug("路由不支持负载均衡，跳过灰度处理: {}", route != null ? route.getUri() : "null");
                return chain.filter(exchange);
            }

            boolean shouldUseCanary = false;
            String reason = "";

            // 策略1: 检查请求头标识
            if (checkCanaryHeader(request)) {
                shouldUseCanary = true;
                reason = "请求头匹配";
            }
            // 策略2: 检查IP白名单
            else if (checkCanaryIp(request)) {
                shouldUseCanary = true;
                reason = "IP白名单匹配";
            }

            // 如果需要使用灰度环境，修改路由
            if (shouldUseCanary) {
                log.info("触发灰度路由 - 原因: {}, 请求路径: {}, 客户端IP: {}", 
                        reason, request.getPath().value(), getClientIp(request));
                
                try {
                    Route newRoute = buildCanaryRoute(route, request);
                    exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, newRoute);
                    log.info("灰度路由设置成功: {} -> {}", route.getUri(), newRoute.getUri());
                } catch (Exception e) {
                    log.error("灰度路由设置失败: {}", e.getMessage(), e);
                    // 设置失败时继续使用原路由
                }
            }

            return chain.filter(exchange);
        };
    }

    /**
     * 检查请求头是否匹配灰度标识
     * 
     * @param request HTTP请求
     * @return 是否匹配灰度标识
     */
    private boolean checkCanaryHeader(ServerHttpRequest request) {
        if (!StringUtils.hasText(canaryHeaderKey) || !StringUtils.hasText(canaryHeaderValues)) {
            return false;
        }

        List<String> headerValues = request.getHeaders().get(canaryHeaderKey);
        if (headerValues == null || headerValues.isEmpty()) {
            return false;
        }

        List<String> canaryValues = Arrays.asList(canaryHeaderValues.split(","));
        
        // 检查请求头值是否在灰度标识列表中
        for (String headerValue : headerValues) {
            if (canaryValues.contains(headerValue.trim())) {
                log.debug("请求头灰度匹配: {}={}", canaryHeaderKey, headerValue);
                return true;
            }
        }

        return false;
    }

    /**
     * 检查客户端IP是否在灰度白名单中
     * 
     * @param request HTTP请求
     * @return 是否在IP白名单中
     */
    private boolean checkCanaryIp(ServerHttpRequest request) {
        if (!StringUtils.hasText(canaryIpWhitelist)) {
            return false;
        }

        String clientIp = getClientIp(request);
        List<String> whitelistIps = Arrays.asList(canaryIpWhitelist.split(","));
        
        boolean isInWhitelist = whitelistIps.stream()
                .anyMatch(ip -> ip.trim().equals(clientIp));
        
        if (isInWhitelist) {
            log.debug("IP灰度匹配: {}", clientIp);
        }
        
        return isInWhitelist;
    }

    /**
     * 构建灰度路由
     * 
     * @param originalRoute 原始路由
     * @param request HTTP请求
     * @return 新的灰度路由
     * @throws URISyntaxException URI构建异常
     */
    private Route buildCanaryRoute(Route originalRoute, ServerHttpRequest request) throws URISyntaxException {
        URI newUri;
        
        // 如果启用外部URL，使用外部地址
        if (externalUrlEnabled && StringUtils.hasText(externalUrlTarget)) {
            newUri = new URI(externalUrlTarget);
            log.debug("使用外部灰度URL: {}", externalUrlTarget);
        } else {
            // 默认策略：在服务名后添加 -canary 后缀
            String originalUriStr = originalRoute.getUri().toString();
            String canaryUriStr = originalUriStr + "-canary";
            newUri = new URI(canaryUriStr);
            log.debug("使用服务名灰度策略: {} -> {}", originalUriStr, canaryUriStr);
        }

        // 构建新的路由对象
        return Route.async()
                .asyncPredicate(originalRoute.getPredicate())
                .filters(originalRoute.getFilters())
                .id(originalRoute.getId() + "-canary")
                .order(originalRoute.getOrder())
                .uri(newUri)
                .build();
    }

    /**
     * 获取客户端真实IP地址
     * 
     * 支持多种代理环境下的IP获取：
     * - X-Forwarded-For: 标准代理头
     * - X-Real-IP: Nginx代理头
     * - Proxy-Client-IP: Apache代理头
     * 
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIp(ServerHttpRequest request) {
        // 优先级顺序检查各种代理头
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeaders().getFirst(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // 如果有多个IP，取第一个
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        // 如果都没有，返回远程地址
        return request.getRemoteAddress() != null ? 
               request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    /**
     * 过滤器配置类
     */
    @Data
    public static class Config {
        /** 是否启用灰度发布 */
        private boolean enabled = true;
        
        /** 灰度权重 (0-100) */
        private int weight = 10;
        
        /** 灰度版本标识 */
        private String version = "canary";
    }
}

