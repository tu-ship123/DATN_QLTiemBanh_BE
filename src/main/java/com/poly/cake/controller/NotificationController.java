package com.poly.cake.controller;

import com.poly.cake.dto.NotificationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notifications")
public class NotificationController {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/send-all")
    public ResponseEntity<?> sendToAllOnlineUsers(@RequestBody NotificationRequest request) {
        // Bắn dữ liệu xuống tất cả user đang subscribe vào channel /topic/public
        messagingTemplate.convertAndSend("/topic/public", request);
        return ResponseEntity.ok("Đã gửi thông báo Popup thành công tới tất cả User online!");
    }
}