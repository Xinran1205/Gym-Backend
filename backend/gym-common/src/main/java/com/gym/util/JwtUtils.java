package com.gym.util;

import com.gym.entity.User;
import io.jsonwebtoken.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 基于 io.jsonwebtoken 0.9.1 的示例
 */
@Component
@Data
public class JwtUtils {

    // 从 application.properties / application.yml 中读取配置
    @Value("${jwt.secret:defaultSecretKey}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}") // 默认1天(毫秒)
    private long expiration;

    /**
     * 生成 JWT Token
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expiration);

        // 在 Subject 存 userId，另外也可放进 Claims
        return Jwts.builder()
                .setSubject(String.valueOf(user.getUserID()))
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    /**
     * 检查 Token 是否有效（签名是否正确 & 是否过期）
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            // 包括 SignatureException, ExpiredJwtException 等
            return false;
        }
    }

    /**
     * 从 token 中获取所有声明 (Claims)
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
    }
}
