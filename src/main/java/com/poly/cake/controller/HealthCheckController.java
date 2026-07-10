package com.poly.cake.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * T103 – Health Check Endpoint
 * GET /api/v1/health → public (không cần token)
 * Kiểm tra trạng thái DB và Redis, trả về JSON thân thiện cho monitoring.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final StringRedisTemplate redisTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        result.put("service", "bakery-3d-api");

        // Kiểm tra Database
        String dbStatus = checkDatabase();
        result.put("database", dbStatus);

        // Kiểm tra Redis
        String redisStatus = checkRedis();
        result.put("redis", redisStatus);

        // Nếu có component DOWN → trả về 503 Service Unavailable
        boolean allHealthy = "OK".equals(dbStatus) && "OK".equals(redisStatus);
        if (!allHealthy) {
            result.put("status", "DEGRADED");
            return ResponseEntity.status(503).body(result);
        }

        return ResponseEntity.ok(result);
    }

    private String checkDatabase() {
        try {
            // Chạy một câu query cực nhẹ để kiểm tra kết nối DB còn sống không
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            return "OK";
        } catch (Exception e) {
            log.error("[HealthCheck] Database DOWN: {}", e.getMessage());
            return "DOWN - " + e.getMessage();
        }
    }

    private String checkRedis() {
        try {
            // Thử set/get một key tạm để xác nhận Redis phản hồi
            redisTemplate.opsForValue().set("_health_ping", "pong");
            String pong = redisTemplate.opsForValue().get("_health_ping");
            redisTemplate.delete("_health_ping");
            return "pong".equals(pong) ? "OK" : "DOWN - unexpected response";
        } catch (Exception e) {
            log.error("[HealthCheck] Redis DOWN: {}", e.getMessage());
            return "DOWN - " + e.getMessage();
        }
    }
}
