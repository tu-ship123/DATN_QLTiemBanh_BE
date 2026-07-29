package com.poly.cake.controller;

import com.poly.cake.service.ChatbotLichSuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Path "/api/v1/admin/**" đã được bảo vệ ROLE_ADMIN sẵn trong CauHinhBaoMat
@RestController
@RequestMapping("/api/v1/admin/chatbot")
@RequiredArgsConstructor
public class AdminChatbotController {

    private final ChatbotLichSuService chatbotHistoryService;

    // Danh sách các phiên chat với bot (mỗi dòng = 1 khách hoặc 1 phiên khách vãng lai)
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations() {
        return ResponseEntity.ok(chatbotHistoryService.getConversations());
    }

    // Toàn bộ lượt hỏi-đáp của 1 phiên chat cụ thể
    @GetMapping("/conversations/{sessionId}")
    public ResponseEntity<?> getHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatbotHistoryService.getHistory(sessionId));
    }
}
