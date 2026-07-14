package com.poly.cake.controller;

import com.poly.cake.entity.NguoiDung;
import com.poly.cake.exception.ResourceNotFoundException;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Path "/api/v1/admin/**" đã được bảo vệ ROLE_ADMIN sẵn trong SecurityConfig
@RestController
@RequestMapping("/api/v1/admin/messages")
@RequiredArgsConstructor
public class AdminMessagesController {

    private final MessageService messageService;
    private final NguoiDungRepository nguoiDungRepository;

    // Hộp thư đến: danh sách hội thoại theo từng khách hàng
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations() {
        return ResponseEntity.ok(messageService.getConversations());
    }

    // Toàn bộ tin nhắn với 1 khách hàng cụ thể
    @GetMapping("/conversations/{khachHangId}")
    public ResponseEntity<?> getMessages(@PathVariable Long khachHangId) {
        return ResponseEntity.ok(messageService.getMessages(khachHangId));
    }

    // Admin trả lời khách hàng
    @PostMapping("/conversations/{khachHangId}/reply")
    public ResponseEntity<?> reply(Authentication authentication, @PathVariable Long khachHangId,
                                    @RequestBody Map<String, String> body) {
        NguoiDung admin = nguoiDungRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại!"));
        return ResponseEntity.ok(messageService.guiTinNhan(khachHangId, admin.getId(), body.get("noiDung"), true));
    }

    // Đánh dấu đã đọc toàn bộ tin nhắn của 1 khách hàng
    @PutMapping("/conversations/{khachHangId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long khachHangId) {
        messageService.danhDauDaDoc(khachHangId);
        return ResponseEntity.ok().build();
    }
}
