package com.poly.cake.controller;

import com.poly.cake.service.DiscordWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

// Path "/api/v1/admin/**" đã được bảo vệ ROLE_ADMIN sẵn trong SecurityConfig
@RestController
@RequestMapping("/api/v1/admin/webhook")
@RequiredArgsConstructor
public class AdminWebhookController {

    private final DiscordWebhookService discordWebhookService;

    // Trạng thái cấu hình webhook Discord hiện tại (không lộ URL thật)
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", discordWebhookService.isEnabled());
        result.put("configured", discordWebhookService.isConfigured());
        result.put("maskedUrl", discordWebhookService.getMaskedUrl());
        return ResponseEntity.ok(result);
    }

    // Gửi 1 tin nhắn test vào kênh Discord để Admin xác nhận webhook hoạt động
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTest() {
        boolean ok = discordWebhookService.sendTestMessage();
        Map<String, Object> result = new HashMap<>();
        result.put("success", ok);
        result.put("message", ok ? "Đã gửi tin nhắn test thành công!" : "Gửi thất bại - kiểm tra lại cấu hình webhook trong application.yml");
        return ResponseEntity.ok(result);
    }
}
