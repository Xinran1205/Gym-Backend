package com.gym.auth.filter;

import com.alibaba.fastjson.JSON;
import com.gym.entity.User;
import com.gym.result.RestResult;
// import com.gym.service.RedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
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
 * API Token认证过滤器
 * 
 * 功能说明：
 * 1. API Token有效性验证
 * 2. 用户身份信息获取和注入
 * 3. URL白名单支持 (ignoreUrl配置)
 * 4. URL黑名单支持 (forbiddenUrl配置)
 * 5. HTTP方法限制 (allowMethod配置)
 * 6. 用户信息传递给下游服务
 * 
 * 认证流程：
 * 1. 检查HTTP方法是否允许
 * 2. 检查URL是否在黑名单中
 * 3. 检查URL是否在白名单中
 * 4. 验证API Token
 * 5. 获取用户信息并注入请求
 * 
 * @author gym-system
 * @version 1.0
 */
@Slf4j
@Component
@Order(2)  // 在JWT过滤器之后执行
public class ApiTokenFilter extends OncePerRequestFilter {

    // @Autowired
    // private RedisCacheService redisCacheService;

    /** 路径匹配器 */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /** 允许的HTTP方法 */
    @Value("${auth.api-token.allow-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    /** 禁止访问的URL模式 */
    @Value("${auth.api-token.forbidden-urls:}")
    private String forbiddenUrls;

    /** 白名单URL模式 (这些URL不需要API Token认证) */
    @Value("${auth.api-token.ignore-urls:/login,/register,/health/**}")
    private String ignoreUrls;

    /** API Token请求头名称 */
    private static final String API_TOKEN_HEADER = "X-API-Token";

    /** 用户ID请求头名称 */
    private static final String USER_ID_HEADER = "X-User-Id";

    /** 用户角色请求头名称 */
    private static final String USER_ROLE_HEADER = "X-User-Role";

    /** 用户邮箱请求头名称 */
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    /**
     * API Token认证过滤器执行逻辑
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
        String apiToken = request.getHeader(API_TOKEN_HEADER);
        String userAgent = request.getHeader("User-Agent");
        
        log.debug("API Token认证过滤器处理请求: {} {} - Token: {}", 
                 method, requestPath, apiToken != null ? "存在" : "缺失");

        // 1. 检查HTTP方法是否允许
        if (!isMethodAllowed(method)) {
            log.warn("API Token认证失败: HTTP方法不允许 - {} {}", method, requestPath);
            handleAuthenticationError(response, "HTTP方法不允许", HttpStatus.METHOD_NOT_ALLOWED.value());
            return;
        }

        // 2. 检查URL是否在黑名单中
        if (isForbiddenUrl(requestPath)) {
            log.warn("API Token认证失败: URL被禁止访问 - {}", requestPath);
            handleAuthenticationError(response, "访问被禁止", HttpStatus.FORBIDDEN.value());
            return;
        }

        // 3. 检查URL是否在白名单中
        if (isIgnoreUrl(requestPath)) {
            log.debug("API Token白名单路径，跳过API Token认证: {}", requestPath);
            
            // 如果用户提供了Token，仍然尝试解析用户信息
            if (StringUtils.hasText(apiToken)) {
                try {
                    User user = validateApiTokenAndGetUser(apiToken, requestPath, userAgent);
                    if (user != null) {
                        injectUserInfo(request, user, apiToken);
                        log.debug("白名单路径用户信息注入成功: {} [{}]", user.getEmail(), user.getRole());
                    }
                } catch (Exception e) {
                    log.debug("白名单路径Token解析失败，继续放行: {}", e.getMessage());
                }
            }
            
            filterChain.doFilter(request, response);
            return;
        }

        // 4. 非白名单路径必须提供API Token
        if (!StringUtils.hasText(apiToken)) {
            log.warn("API Token认证失败: 缺少API Token - {}", requestPath);
            handleAuthenticationError(response, "缺少API Token", HttpStatus.UNAUTHORIZED.value());
            return;
        }

        try {
            // 5. 验证API Token并获取用户信息
            User user = validateApiTokenAndGetUser(apiToken, requestPath, userAgent);
            
            if (user == null) {
                log.warn("API Token认证失败: Token无效或用户不存在 - {}", requestPath);
                handleAuthenticationError(response, "API Token无效", HttpStatus.UNAUTHORIZED.value());
                return;
            }

            // 6. 将用户信息注入到请求中
            injectUserInfo(request, user, apiToken);
            
            log.info("✅ API Token认证通过 - 用户: {} [{}] - {} {}", 
                    user.getEmail(), user.getRole(), method, requestPath);

        } catch (Exception e) {
            log.error("API Token认证异常 - 请求路径: {}, 错误: {}", requestPath, e.getMessage());
            handleAuthenticationError(response, "Token验证失败", HttpStatus.UNAUTHORIZED.value());
            return;
        }

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 检查HTTP方法是否允许
     * 
     * @param method HTTP方法
     * @return 是否允许
     */
    private boolean isMethodAllowed(String method) {
        if (!StringUtils.hasText(allowedMethods)) {
            return true; // 如果没有配置限制，则允许所有方法
        }
        
        List<String> allowedMethodList = Arrays.asList(allowedMethods.toUpperCase().split(","));
        return allowedMethodList.contains(method.toUpperCase().trim());
    }

    /**
     * 检查URL是否在黑名单中
     * 
     * @param path 请求路径
     * @return 是否为禁止访问的路径
     */
    private boolean isForbiddenUrl(String path) {
        if (!StringUtils.hasText(forbiddenUrls)) {
            return false;
        }
        
        List<String> forbiddenUrlList = Arrays.asList(forbiddenUrls.split(","));
        return forbiddenUrlList.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern.trim(), path));
    }

    /**
     * 检查URL是否在白名单中
     * 
     * @param path 请求路径
     * @return 是否为白名单路径
     */
    private boolean isIgnoreUrl(String path) {
        if (!StringUtils.hasText(ignoreUrls)) {
            return false;
        }
        
        List<String> ignoreUrlList = Arrays.asList(ignoreUrls.split(","));
        return ignoreUrlList.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern.trim(), path));
    }

    /**
     * 验证API Token并获取用户信息
     * 
     * @param apiToken API Token
     * @param requestPath 请求路径
     * @param userAgent 用户代理
     * @return 用户信息
     */
    private User validateApiTokenAndGetUser(String apiToken, String requestPath, String userAgent) {
        try {
            // 这里实现API Token验证逻辑
            // 1. 可以基于Token格式验证
            // 2. 可以查询数据库验证Token有效性
            // 3. 可以从Redis缓存中获取用户信息
            
            // 简化实现：假设API Token格式为 "user_{userId}"
            if (apiToken.startsWith("user_")) {
                String userIdStr = apiToken.substring(5);
                try {
                    Long userId = Long.valueOf(userIdStr);
                    
                    // 从缓存中获取用户信息
                    // User user = redisCacheService.getUser(userId);
                    User user = null; // 临时注释掉Redis功能
                    
                    if (user != null && user.getAccountStatus() == User.AccountStatus.Approved) {
                        log.debug("API Token验证成功 - 用户ID: {}, 邮箱: {}", userId, user.getEmail());
                        return user;
                    } else {
                        log.warn("API Token验证失败 - 用户不存在或未激活: {}", userId);
                    }
                    
                } catch (NumberFormatException e) {
                    log.warn("API Token格式错误 - 无法解析用户ID: {}", userIdStr);
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("API Token验证异常: {}", e.getMessage());
            throw new RuntimeException("Token验证失败", e);
        }
    }

    /**
     * 将用户信息注入到请求中
     * 
     * @param request HTTP请求
     * @param user 用户信息
     * @param apiToken API Token
     */
    private void injectUserInfo(HttpServletRequest request, User user, String apiToken) {
        // 注入用户信息到请求属性
        request.setAttribute("userId", user.getUserID().toString());
        request.setAttribute("userRole", user.getRole().name());
        request.setAttribute("userEmail", user.getEmail());
        request.setAttribute("userName", user.getName());
        request.setAttribute("apiToken", apiToken);
        
        // 注入用户信息到请求头 (用于传递给下游服务)
        // 注意：这里使用包装器来添加请求头
        HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request);
        wrapper.addHeader(USER_ID_HEADER, user.getUserID().toString());
        wrapper.addHeader(USER_ROLE_HEADER, user.getRole().name());
        wrapper.addHeader(USER_EMAIL_HEADER, user.getEmail());
        
        log.debug("用户信息注入完成 - ID: {}, 角色: {}, 邮箱: {}", 
                 user.getUserID(), user.getRole(), user.getEmail());
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
        result.add("authType", "API_TOKEN");
        
        // 写入响应体
        String jsonResult = JSON.toJSONString(result);
        response.getWriter().write(jsonResult);
        response.getWriter().flush();
        
        log.warn("❌ API Token认证拒绝 - 状态码: {}, 消息: {}", statusCode, message);
    }

    /**
     * HTTP请求包装器，用于添加请求头
     */
    private static class HttpServletRequestWrapper extends javax.servlet.http.HttpServletRequestWrapper {
        private final java.util.Map<String, String> customHeaders = new java.util.HashMap<>();

        public HttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        public void addHeader(String name, String value) {
            customHeaders.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String headerValue = customHeaders.get(name);
            if (headerValue != null) {
                return headerValue;
            }
            return super.getHeader(name);
        }

        @Override
        public java.util.Enumeration<String> getHeaderNames() {
            java.util.Set<String> set = new java.util.HashSet<>(customHeaders.keySet());
            java.util.Enumeration<String> e = super.getHeaderNames();
            while (e.hasMoreElements()) {
                set.add(e.nextElement());
            }
            return java.util.Collections.enumeration(set);
        }
    }
}

