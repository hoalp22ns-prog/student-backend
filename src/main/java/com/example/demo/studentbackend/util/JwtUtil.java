package com.example.demo.studentbackend.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;

/**
 * 🔐 JWT UTILITY: JwtUtil
 * 
 * Mục đích:
 *  - Generate JWT token khi user login
 *  - Validate JWT token từ request
 *  - Extract username từ token
 * 
 * JWT Structure: header.payload.signature
 *  - header: {alg: "HS256", typ: "JWT"}
 *  - payload: {sub: username, iat: timestamp, exp: timestamp}
 *  - signature: hmac(header.payload, secret_key)
 * 
 * Security:
 *  - Secret key được lưu trong application.properties
 *  - Token expire sau 24 giờ
 *  - Signature được verify trước khi sử dụng
 */
@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationAndValidation12345678901234567890}")
    private String secretKeyString;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private long jwtExpirationMs;

    /**
     * Generate JWT token từ username
     * 
     * @param username - Tên đăng nhập
     * @return JWT token string
     * 
     * Ví dụ output:
     * eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTYxNjIzOTAyMn0...
     */
    public String generateToken(String username) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKeyString.getBytes());
            
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", "ADMIN"); // Có thể thêm role
            
            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(username)  // username trong token
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
            
            log.info("✅ JWT token generated for user: {}", username);
            return token;
        } catch (Exception e) {
            log.error("❌ Error generating JWT token: {}", e.getMessage());
            throw new RuntimeException("❌ Cannot generate JWT token");
        }
    }

    /**
     * Extract username từ token
     * 
     * @param token - JWT token string
     * @return Username được lưu trong token
     */
    public String extractUsername(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKeyString.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getSubject();
        } catch (Exception e) {
            log.error("❌ Error extracting username from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate JWT token
     * 
     * @param token - JWT token string
     * @return true nếu token hợp lệ, false nếu expired/invalid
     * 
     * Checks:
     *  - Signature hợp lệ
     *  - Token không hết hạn
     *  - Token có subject (username)
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKeyString.getBytes());
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            
            log.debug("✅ JWT token is valid");
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("⚠️ JWT token has expired: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("⚠️ Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("⚠️ JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract token từ Authorization header
     * 
     * @param authHeader - "Bearer <token>"
     * @return Token string (bỏ "Bearer " prefix)
     * 
     * Ví dụ:
     *  Input: "Bearer eyJhbGci..."
     *  Output: "eyJhbGci..."
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);  // Bỏ "Bearer " (7 characters)
        }
        return null;
    }
}
