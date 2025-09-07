package com.gym.auth.filter;

import com.alibaba.fastjson.JSON;
import com.gym.result.RestResult;
import lombok.extern.slf4j.Slf4j;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * IP白名单认证过滤器
 * 
 * 功能说明：
 * 1. 支持多种IP格式配置
 *    - 单个IP: 192.168.1.1
 *    - IP范围: 192.168.1.1-192.168.1.100
 *    - 通配符: 192.168.*.*
 *    - 网段: 192.168.1.0/24
 *    - 混合规则: 多种格式组合 (分号分隔)
 * 2. 特殊标识支持
 *    - "0.0": 允许所有IP
 *    - "0": 禁止所有IP
 * 3. 获取真实IP地址 (支持代理环境)
 * 4. 灵活的路径匹配规则
 * 
 * IP白名单设计关键：
 * - 包含多个规则，使用分号分隔
 * - 支持复杂的网络拓扑
 * - 处理多层代理环境
 * - 提供详细的访问日志
 * 
 * @author gym-system
 * @version 1.0
 */
@Slf4j
@Component
@Order(3)  // 在其他认证过滤器之后执行
public class IpWhitelistFilter extends OncePerRequestFilter {

    /** IP地址正则表达式 */
    private static final Pattern IP_PATTERN = Pattern
            .compile("(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\." + 
                    "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\." +
                    "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\." + 
                    "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})");

    /** 允许所有IP的标识 */
    private static final String ALLOW_ALL_FLAG = "0.0";
    
    /** 禁止所有IP的标识 */
    private static final String DENY_ALL_FLAG = "0";

    /** 路径匹配器 */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /** 是否启用IP白名单验证 */
    @Value("${auth.ip-whitelist.enabled:false}")
    private boolean ipWhitelistEnabled;

    /** IP白名单配置 */
    @Value("${auth.ip-whitelist.allowed-ips:127.0.0.1,::1}")
    private String allowedIps;

    /** 需要IP白名单验证的路径模式 */
    @Value("${auth.ip-whitelist.protected-paths:/admin/**,/internal/**}")
    private String protectedPaths;

    /** 白名单豁免路径 (这些路径不进行IP验证) */
    @Value("${auth.ip-whitelist.exempt-paths:/health/**,/actuator/**}")
    private String exemptPaths;

    /**
     * IP白名单认证过滤器执行逻辑
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
        
        log.debug("IP白名单过滤器处理请求: {} {} - 启用状态: {}", 
                 method, requestPath, ipWhitelistEnabled);

        // 如果未启用IP白名单验证，直接放行
        if (!ipWhitelistEnabled) {
            log.debug("IP白名单验证未启用，直接放行: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // 检查是否为豁免路径
        if (isExemptPath(requestPath)) {
            log.debug("IP白名单豁免路径，跳过验证: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // 检查是否为受保护路径
        if (!isProtectedPath(requestPath)) {
            log.debug("非受保护路径，跳过IP验证: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // 获取客户端真实IP
        String clientIp = getClientIp(request);
        
        log.info("🔍 IP白名单验证 - 客户端IP: {} - 请求: {} {}", clientIp, method, requestPath);

        // 验证IP是否在白名单中
        if (!isIpAllowed(clientIp)) {
            log.warn("❌ IP白名单验证失败 - 客户端IP: {} - 请求: {} {} - 用户代理: {}", 
                    clientIp, method, requestPath, request.getHeader("User-Agent"));
            handleIpDenied(response, clientIp);
            return;
        }

        log.info("✅ IP白名单验证通过 - 客户端IP: {} - 请求: {} {}", clientIp, method, requestPath);

        // 将客户端IP注入到请求属性中
        request.setAttribute("clientIp", clientIp);
        request.setAttribute("ipWhitelistPassed", true);

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 检查路径是否为豁免路径
     * 
     * @param path 请求路径
     * @return 是否为豁免路径
     */
    private boolean isExemptPath(String path) {
        if (!StringUtils.hasText(exemptPaths)) {
            return false;
        }
        
        List<String> exemptPathList = Arrays.asList(exemptPaths.split(","));
        return exemptPathList.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern.trim(), path));
    }

    /**
     * 检查路径是否为受保护路径
     * 
     * @param path 请求路径
     * @return 是否为受保护路径
     */
    private boolean isProtectedPath(String path) {
        if (!StringUtils.hasText(protectedPaths)) {
            return false; // 如果没有配置受保护路径，则不进行IP验证
        }
        
        List<String> protectedPathList = Arrays.asList(protectedPaths.split(","));
        return protectedPathList.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern.trim(), path));
    }

    /**
     * 验证IP是否被允许
     * 
     * @param ip 客户端IP地址
     * @return 是否允许访问
     */
    private boolean isIpAllowed(String ip) {
        if (ip == null || ip.isEmpty()) {
            log.warn("无法获取客户端IP地址");
            return false;
        }

        // IP格式验证
        if (!IP_PATTERN.matcher(ip).matches()) {
            log.warn("IP地址格式不正确: {}", ip);
            return false;
        }

        // 特殊标识处理
        if (ALLOW_ALL_FLAG.equals(allowedIps)) {
            log.debug("允许所有IP访问");
            return true;
        }

        if (DENY_ALL_FLAG.equals(allowedIps)) {
            log.debug("禁止所有IP访问");
            return false;
        }

        // 获取IP白名单
        Set<String> ipWhitelist = getAvailableIpList(allowedIps);
        
        // 验证IP是否在白名单中
        return isIpInWhitelist(ip, ipWhitelist);
    }

    /**
     * 根据IP白名单配置获取可用的IP列表
     * 
     * @param allowIp IP白名单配置字符串
     * @return IP白名单集合
     */
    private Set<String> getAvailableIpList(String allowIp) {
        String[] splitRules = allowIp.split(";"); // 拆分出白名单规则
        Set<String> ipList = new HashSet<>();
        
        for (String rule : splitRules) {
            rule = rule.trim();
            
            if (rule.contains("*")) { 
                // 处理通配符 *
                processWildcardRule(rule, ipList);
            } else if (rule.contains("/")) { 
                // 处理网段 xxx.xxx.xxx.xxx/24
                ipList.add(rule);
            } else { 
                // 处理单个IP或IP范围
                if (validateIpRule(rule)) {
                    ipList.add(rule);
                }
            }
        }
        
        log.debug("解析IP白名单规则完成，共 {} 条规则", ipList.size());
        return ipList;
    }

    /**
     * 处理通配符规则
     * 
     * @param rule 通配符规则
     * @param ipList IP列表
     */
    private void processWildcardRule(String rule, Set<String> ipList) {
        String[] ips = rule.split("\\.");
        String[] from = new String[] { "0", "0", "0", "0" };
        String[] end = new String[] { "255", "255", "255", "255" };
        List<String> wildcardRanges = new ArrayList<>();
        
        for (int i = 0; i < ips.length && i < 4; i++) {
            if (ips[i].contains("*")) {
                List<String> ranges = completeWildcard(ips[i]);
                wildcardRanges = ranges;
                from[i] = null;
                end[i] = null;
            } else {
                from[i] = ips[i];
                end[i] = ips[i];
            }
        }

        // 构建IP范围
        StringBuilder fromIP = new StringBuilder();
        StringBuilder endIP = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (from[i] != null) {
                fromIP.append(from[i]).append(".");
                endIP.append(end[i]).append(".");
            } else {
                fromIP.append("[*].");
                endIP.append("[*].");
            }
        }
        
        if (fromIP.length() > 0) {
            fromIP.deleteCharAt(fromIP.length() - 1);
            endIP.deleteCharAt(endIP.length() - 1);
        }

        for (String range : wildcardRanges) {
            String[] parts = range.split(";");
            if (parts.length == 2) {
                String ipRange = fromIP.toString().replace("[*]", parts[0]) + "-"
                        + endIP.toString().replace("[*]", parts[1]);
                if (validateIpRule(ipRange)) {
                    ipList.add(ipRange);
                }
            }
        }
    }

    /**
     * 对单个IP节点进行范围限定
     * 
     * @param arg 通配符参数
     * @return 返回限定后的IP范围，格式为List["10;19", "100;199"]
     */
    private List<String> completeWildcard(String arg) {
        List<String> ranges = new ArrayList<>();
        int len = arg.length();
        
        if (len == 1) {
            ranges.add("0;255");
        } else if (len == 2) {
            String s1 = completeWildcardRange(arg, 1);
            if (s1 != null) ranges.add(s1);
            String s2 = completeWildcardRange(arg, 2);
            if (s2 != null) ranges.add(s2);
        } else {
            String s1 = completeWildcardRange(arg, 1);
            if (s1 != null) ranges.add(s1);
        }
        
        return ranges;
    }

    /**
     * 完成通配符范围计算
     * 
     * @param arg 通配符参数
     * @param length 长度
     * @return 范围字符串
     */
    private String completeWildcardRange(String arg, int length) {
        String from, end;
        
        if (length == 1) {
            from = arg.replace("*", "0");
            end = arg.replace("*", "9");
        } else {
            from = arg.replace("*", "00");
            end = arg.replace("*", "99");
        }
        
        try {
            if (Integer.parseInt(from) > 255) {
                return null;
            }
            if (Integer.parseInt(end) > 255) {
                end = "255";
            }
        } catch (NumberFormatException e) {
            return null;
        }
        
        return from + ";" + end;
    }

    /**
     * 验证IP规则格式
     * 
     * @param rule IP规则
     * @return 是否有效
     */
    private boolean validateIpRule(String rule) {
        String[] parts = rule.split("-");
        for (String part : parts) {
            if (!IP_PATTERN.matcher(part.trim()).matches()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查IP是否在白名单中
     * 
     * @param ip 客户端IP
     * @param ipWhitelist IP白名单
     * @return 是否在白名单中
     */
    private boolean isIpInWhitelist(String ip, Set<String> ipWhitelist) {
        if (ipWhitelist.isEmpty() || ipWhitelist.contains(ip)) {
            return true;
        }

        for (String rule : ipWhitelist) {
            if (rule.contains("-")) { 
                // 处理IP范围 192.168.0.0-192.168.2.1
                if (isIpInRange(ip, rule)) {
                    return true;
                }
            } else if (rule.contains("/")) { 
                // 处理网段 xxx.xxx.xxx.xxx/24
                if (isIpInSubnet(ip, rule)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * 检查IP是否在指定范围内
     * 
     * @param ip 客户端IP
     * @param range IP范围 (格式: start-end)
     * @return 是否在范围内
     */
    private boolean isIpInRange(String ip, String range) {
        try {
            String[] rangeParts = range.split("-");
            if (rangeParts.length != 2) {
                return false;
            }
            
            String[] startParts = rangeParts[0].trim().split("\\.");
            String[] endParts = rangeParts[1].trim().split("\\.");
            String[] ipParts = ip.split("\\.");
            
            if (startParts.length != 4 || endParts.length != 4 || ipParts.length != 4) {
                return false;
            }
            
            // 对IP从左到右进行逐段匹配
            for (int i = 0; i < 4; i++) {
                int start = Integer.parseInt(startParts[i]);
                int end = Integer.parseInt(endParts[i]);
                int current = Integer.parseInt(ipParts[i]);
                
                if (!(start <= current && current <= end)) {
                    return false;
                }
            }
            
            return true;
            
        } catch (NumberFormatException e) {
            log.warn("IP范围解析失败: {}", range);
            return false;
        }
    }

    /**
     * 检查IP是否在指定网段内
     * 
     * @param ip 客户端IP
     * @param subnet 网段 (格式: xxx.xxx.xxx.xxx/24)
     * @return 是否在网段内
     */
    private boolean isIpInSubnet(String ip, String subnet) {
        try {
            int slashIndex = subnet.indexOf("/");
            if (slashIndex == -1) {
                return false;
            }
            
            String networkAddress = subnet.substring(0, slashIndex);
            int prefixLength = Integer.parseInt(subnet.substring(slashIndex + 1));
            
            // IP转为长整型
            long ipLong = ipToLong(ip);
            long networkLong = ipToLong(networkAddress);
            
            // 计算子网掩码
            long maskLong = (0xFFFFFFFFL << (32 - prefixLength)) & 0xFFFFFFFFL;
            
            // 计算网络地址
            long calculatedNetwork = ipLong & maskLong;
            long expectedNetwork = networkLong & maskLong;
            
            return calculatedNetwork == expectedNetwork;
            
        } catch (Exception e) {
            log.warn("网段匹配失败: {} - {}", ip, subnet);
            return false;
        }
    }

    /**
     * IP地址转换为长整型
     * 
     * @param ip IP地址字符串
     * @return 长整型值
     */
    private long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) + Integer.parseInt(parts[i]);
        }
        return result;
    }

    /**
     * 获取客户端真实IP地址
     * 
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        // 尝试从各种代理头中获取真实IP
        String[] headers = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", 
            "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // 如果有多个IP，取第一个
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        // 如果都没有，返回远程地址
        return request.getRemoteAddr();
    }

    /**
     * 处理IP访问被拒绝
     * 
     * @param response HTTP响应
     * @param clientIp 客户端IP
     * @throws IOException IO异常
     */
    private void handleIpDenied(HttpServletResponse response, String clientIp) throws IOException {
        // 设置响应状态码和内容类型
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        // 构建错误响应
        RestResult<Object> result = RestResult.error("IP地址不在白名单中，访问被拒绝", HttpStatus.FORBIDDEN.value());
        result.add("clientIp", clientIp);
        result.add("timestamp", System.currentTimeMillis());
        result.add("authType", "IP_WHITELIST");
        
        // 写入响应体
        String jsonResult = JSON.toJSONString(result);
        response.getWriter().write(jsonResult);
        response.getWriter().flush();
    }
}

