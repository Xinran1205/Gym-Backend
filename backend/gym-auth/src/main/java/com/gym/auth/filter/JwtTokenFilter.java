package com.gym.auth.filter;

import com.alibaba.fastjson.JSON;
import com.gym.result.RestResult;
import com.gym.util.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * JWT Token认证过滤器
 * 
 * 功能说明：
 * 1. Bearer Token格式验证
 * 2. JWT签名验证
 * 3. Token过期时间检查
 * 4. 用户信息提取和注入
 * 5. 白名单路径放行
 * 
 * 认证流程：
 * 1. 检查请求路径是否在白名单中
 * 2. 提取Authorization头中的Bearer Token
 * 3. 验证Token的有效性和完整性
 * 4. 解析Token获取用户信息
 * 5. 将用户信息注入到请求属性中
 * 
 * @author gym-system
 * @version 1.0
 */
@Slf4j
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    /** 路径匹配器 */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /** 
     * JWT Token认证白名单路径
     * 这些路径不需要JWT Token认证
     */
    private final List<String> jwtWhitelistPaths = Arrays.asList(
            "/login",                    // 用户登录
            "/register",                 // 用户注册  
            "/verify-code",              // 验证码验证
            "/forgot-password",          // 忘记密码
            "/reset-password",           // 重置密码
            "/google-login",             // Google登录
            "/token",                    // Token生成接口
            "/health/**",                // 健康检查
            "/actuator/**",              // 监控端点
            "/doc.html",                 // API文档
            "/swagger-resources/**",     // Swagger资源
            "/webjars/**",               // Web资源
            "/v2/api-docs",              // API文档
            "/static/**"                 // 静态资源
    );

    /**
     * JWT Token认证过滤器执行逻辑
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        
        log.debug("JWT认证过滤器处理请求: {} {}", method, requestPath);

        // 检查是否为白名单路径
        if (isJwtWhitelistPath(requestPath)) {
            log.debug("JWT白名单路径，跳过JWT认证: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // 提取Authorization头
        String authHeader = request.getHeader("Authorization");
        
        // 检查Authorization头是否存在
        if (!StringUtils.hasText(authHeader)) {
            log.warn("JWT认证失败: 缺少Authorization头 - 请求路径: {}", requestPath);
            handleAuthenticationError(response, "缺少认证信息", HttpStatus.UNAUTHORIZED.value());
            return;
        }

        // 检查Bearer Token格式
        if (!authHeader.startsWith("Bearer ")) {
            log.warn("JWT认证失败: Authorization头格式错误 - 请求路径: {}", requestPath);
            handleAuthenticationError(response, "认证格式错误", HttpStatus.UNAUTHORIZED.value());
            return;
        }

        // 提取JWT Token
        String token = authHeader.substring(7);
        
        try {
            // 验证Token有效性
            if (!jwtUtils.validateToken(token)) {
                log.warn("JWT认证失败: Token无效 - 请求路径: {}", requestPath);
                handleAuthenticationError(response, "Token无效或已过期", HttpStatus.UNAUTHORIZED.value());
                return;
            }

            // 解析Token获取用户信息
            Claims claims = jwtUtils.getClaims(token);
            String userId = claims.getSubject();
            String role = (String) claims.get("role");
            String email = (String) claims.get("email");

            log.debug("JWT认证成功 - 用户ID: {}, 角色: {}, 邮箱: {} - 请求路径: {}", 
                     userId, role, email, requestPath);

            // 将用户信息注入到请求属性中，供后续业务逻辑使用
            request.setAttribute("userId", userId);
            request.setAttribute("userRole", role);
            request.setAttribute("userEmail", email);
            request.setAttribute("authToken", token);
            
            // 记录成功的认证日志
            log.info("✅ JWT认证通过 - 用户: {} [{}] - {} {}", 
                    email, role, method, requestPath);

        } catch (Exception e) {
            log.error("JWT认证异常 - 请求路径: {}, 错误: {}", requestPath, e.getMessage());
            handleAuthenticationError(response, "Token解析失败", HttpStatus.UNAUTHORIZED.value());
            return;
        }

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 检查路径是否在JWT白名单中
     * 
     * @param path 请求路径
     * @return 是否为白名单路径
     */
    private boolean isJwtWhitelistPath(String path) {
        return jwtWhitelistPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 处理认证错误
     * 
     * @param response HTTP响应
     * @param message 错误消息
     * @param statusCode 状态码
     * @throws IOException IO异常
     */
    private void handleAuthenticationError(HttpServletResponse response, 
                                         String message, 
                                         int statusCode) throws IOException {
        // 设置响应状态码和内容类型
        response.setStatus(statusCode);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        // 构建错误响应
        RestResult<Object> result = RestResult.error(message, statusCode);
        result.add("timestamp", System.currentTimeMillis());
        result.add("authType", "JWT");
        
        // 写入响应体
        String jsonResult = JSON.toJSONString(result);
        response.getWriter().write(jsonResult);
        response.getWriter().flush();
        
        log.warn("❌ JWT认证拒绝 - 状态码: {}, 消息: {}", statusCode, message);
    }
}

