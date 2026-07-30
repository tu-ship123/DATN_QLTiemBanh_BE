package com.poly.cake.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service

public class DichVuTokenRedis {

    private final StringRedisTemplate redisTemplate;

    public DichVuTokenRedis(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Đưa token vào danh sách đen (Blacklist) với thời gian hết hạn
    public void blacklistToken(String token, long expirationTimeInMillis) {
        redisTemplate.opsForValue().set(token, "blacklisted", expirationTimeInMillis, TimeUnit.MILLISECONDS);
    }

    // Kiểm tra xem token có bị khóa không
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(token));
    }
}