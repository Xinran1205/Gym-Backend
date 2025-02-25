package com.gym.util;

import com.gym.entity.User;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class JwtUtilsTest {
    private JwtUtils jwtUtils;

    @BeforeEach
    public void setUp() {
        jwtUtils = new JwtUtils();
        // 模拟注入配置属性，注意：如果 secretKey 和 expiration 属性为 private，
        // 可考虑在 JwtUtils 类中添加包级别或 public 的 setter 方法，或者使用反射设置。

        jwtUtils.setSecretKey("testSecretKey");
        jwtUtils.setExpiration(3600000); // 1 小时的过期时间，单位：毫秒
        log.debug("JwtUtilsTest setup done");
    }

    @Test
    public void testGenerateAndValidateToken() {
        // 构造一个模拟的用户对象
        User user = new User();
        user.setUserID(100L);
        user.setRole(User.Role.member); // 假设 User 类中有 Role 枚举
        user.setEmail("test@example.com");

        // 生成 Token
        String token = jwtUtils.generateToken(user);
        assertNotNull(token, "生成的 Token 不应为空");
        log.debug("Generated token: {}", token);

        // 验证 Token
        boolean isValid = jwtUtils.validateToken(token);
        assertTrue(isValid, "生成的 Token 应该是有效的");
        log.debug("Token is valid: {}", isValid);

        // 从 Token 中获取 Claims，并校验包含的信息
        Claims claims = jwtUtils.getClaims(token);
        assertEquals(String.valueOf(user.getUserID()), claims.getSubject(), "Subject 应该为用户ID");
        assertEquals(user.getRole().name(), claims.get("role"), "角色信息应一致");
        assertEquals(user.getEmail(), claims.get("email"), "邮箱信息应一致");
        log.debug("Claims: {}", claims);
    }

    @Test
    public void testInvalidToken() {
        // 构造一个模拟的用户对象
        User user = new User();
        user.setUserID(101L);
        user.setRole(User.Role.member);
        user.setEmail("test2@example.com");

        // 生成 Token
        String token = jwtUtils.generateToken(user);
        assertNotNull(token, "生成的 Token 不应为空");

        // 模拟篡改 Token，例如改变 Token 的最后一个字符
        String tamperedToken = token.substring(0, token.length() - 1) + "x";
        boolean isValid = jwtUtils.validateToken(tamperedToken);
        assertFalse(isValid, "被篡改的 Token 应该被判定为无效");

        log.debug("Tampered token is invalid: {}", isValid);
    }

}