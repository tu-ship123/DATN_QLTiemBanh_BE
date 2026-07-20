package com.poly.cake.controller;

import com.poly.cake.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * API cho trợ lý ảo (AI chatbot) tư vấn khách hàng — khác với MessagesController
 * (nhắn tin thật với nhân viên). Public — khách vãng lai (chưa đăng nhập) vẫn
 * dùng được để hỏi về menu/chính sách trước khi mua.
 */
@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping(value = "/ask", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> ask(
            @RequestParam("prompt") String prompt,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication authentication) {

        // Nếu khách đã đăng nhập -> dùng email làm sessionId để tách lịch sử hội thoại
        // theo từng người; khách vãng lai dùng chung 1 phiên đơn giản.
        String sessionId = authentication != null ? authentication.getName() : "guest";

        Map<String, String> result = chatbotService.ask(prompt, sessionId, file);
        return ResponseEntity.ok(result);
    }
}
