package com.poly.cake.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    // Access Token: 15 phút (15 * 60 * 1000 = 900,000 ms)
    private static final long ACCESS_TOKEN_EXPIRATION = 900000;

    // Refresh Token: 7 ngày (7 * 24 * 60 * 60 * 1000 = 604,800,000 ms)
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000;

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", "ROLE_" + role) // Spring Security yêu cầu prefix ROLE_
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    private Claims extractAllClaims(String token) {
        return io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(getSignInKey()) // Đảm bảo trỏ đúng vào hàm hoặc biến SecretKey của em
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationTime(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token).getBody();
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }
    public List<GrantedAuthority> extractRoles(String token) {
        // 1. Gọi hàm giải mã Payload có sẵn của em (Thường các form chuẩn đều đặt tên là extractAllClaims)
        Claims claims = extractAllClaims(token);

        // 2. Trích xuất giá trị role ra.
        // Lưu ý: Nếu lúc tạo token em đặt key là "roles" hay "authorities" thì sửa lại chữ "role" cho khớp nhé.
        String role = claims.get("role", String.class);

        // 3. Đóng gói thành GrantedAuthority theo đúng chuẩn Spring Security yêu cầu
        if (role != null && !role.trim().isEmpty()) {
            return Collections.singletonList(new SimpleGrantedAuthority(role));
        }

        return Collections.emptyList();
    }
}