package com.poly.cake.controller;

import com.poly.cake.dto.NotificationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notifications")
// LƯU Ý BẢO MẬT: path "/api/admin/..." (thiếu "v1") không khớp rule "/api/v1/admin/**"
// trong SecurityConfig -> bắt buộc phải có @PreAuthorize ở đây, nếu không bất kỳ user
// đã đăng nhập nào cũng gửi được thông báo popup tới toàn bộ hệ thống.
@PreAuthorize("hasRole('ADMIN')")
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