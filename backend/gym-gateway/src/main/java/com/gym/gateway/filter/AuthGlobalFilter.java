package com.gym.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.gym.result.RestResult;
import com.gym.util.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 认证全局过滤器
 * 
 * 功能说明：
 * 1. JWT Token验证
 * 2. 用户身份信息提取
 * 3. 请求头信息注入
 * 4. 白名单路径放行
 * 
 * 执行顺序：在路由过滤器之前执行
 * 
 * @author gym-system
 * @version 1.0
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtils jwtUtils;

    /** 路径匹配器 */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /** 
     * 白名单路径 - 这些路径不需要认证
     * 统一管理所有不需要认证的路径
     */
    private final List<String> whitelistPaths = Arrays.asList(
            // 认证相关接口 (gym-auth 服务)
            "/auth/**",              // 所有认证相关接口
            
            // 业务服务中的公开接口 (gym-server 服务)
            "/api/user/signup",      // 用户注册
            "/api/user/verify-code", // 验证码验证
            "/api/user/login",       // 用户登录
            "/api/user/forgot-password", // 忘记密码
            "/api/user/reset-password",  // 重置密码
            "/api/user/google-login",    // Google登录
            
            // 系统接口
            "/health/**",            // 健康检查
            "/actuator/**",          // 监控端点
            
            // API文档 (开发环境)
            "/doc.html",             // API文档首页
            "/swagger-resources/**", // Swagger资源
            "/webjars/**",           // Web资源
            "/v2/api-docs",          // API文档接口
            "/static/**",            // 静态资源
            
            // 网关路由端点（用于查看路由信息）
            "/actuator/gateway/**"   // 网关路由查看端点
    );

    /**
     * 过滤器执行逻辑
     * 
     * @param exchange 服务器交换对象
     * @param chain 过滤器链
     * @return Mono<Void> 异步结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        
        log.info("认证过滤器处理请求: {} {}", request.getMethod(), path);

        // 检查是否为白名单路径
        if (isWhitelistPath(path)) {
            log.info("白名单路径，跳过认证: {}", path);
            return chain.filter(exchange);
        }

        // 提取Authorization头
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        // 检查Authorization头是否存在
        if (!StringUtils.hasText(authHeader)) {
            log.warn("缺少Authorization头，请求路径: {}", path);
            return handleUnauthorized(exchange, "缺少认证信息");
        }

        // 检查Bearer Token格式
        if (!authHeader.startsWith("Bearer ")) {
            log.warn("Authorization头格式错误，请求路径: {}", path);
            return handleUnauthorized(exchange, "认证格式错误");
        }

        // 提取JWT Token
        String token = authHeader.substring(7);
        
        try {
            // 验证Token有效性
            if (!jwtUtils.validateToken(token)) {
                log.warn("Token验证失败，请求路径: {}", path);
                return handleUnauthorized(exchange, "Token无效或已过期");
            }

            // 解析Token获取用户信息
            Claims claims = jwtUtils.getClaims(token);
            String userId = claims.getSubject();
            String role = (String) claims.get("role");
            String email = (String) claims.get("email");

            log.debug("Token验证成功 - 用户ID: {}, 角色: {}, 邮箱: {}", userId, role, email);

            // 将用户信息注入到请求头中，传递给下游服务
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)           // 用户ID
                    .header("X-User-Role", role)           // 用户角色
                    .header("X-User-Email", email)         // 用户邮箱
                    .header("X-Auth-Token", token)         // 原始Token
                    .build();

            // 创建新的交换对象
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();

            return chain.filter(mutatedExchange);

        } catch (Exception e) {
            log.error("Token解析异常，请求路径: {}, 错误: {}", path, e.getMessage());
            return handleUnauthorized(exchange, "Token解析失败");
        }
    }

    /**
     * 检查路径是否在白名单中
     * 
     * @param path 请求路径
     * @return 是否为白名单路径
     */
    private boolean isWhitelistPath(String path) {
        return whitelistPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 处理未授权请求
     * 
     * @param exchange 服务器交换对象
     * @param message 错误消息
     * @return Mono<Void> 异步结果
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        
        // 设置响应状态码
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        
        // 设置响应头
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        
        // 构建错误响应
        RestResult<Object> result = RestResult.error(message, HttpStatus.UNAUTHORIZED.value());
        String jsonResult = JSON.toJSONString(result);
        
        // 写入响应体
        DataBuffer buffer = response.bufferFactory().wrap(jsonResult.getBytes(StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 过滤器执行顺序
     * 
     * 数值越小，优先级越高
     * 认证过滤器需要在业务过滤器之前执行
     * 
     * @return 执行顺序
     */
    @Override
    public int getOrder() {
        return -100; // 高优先级，确保在其他业务过滤器之前执行
    }
}

