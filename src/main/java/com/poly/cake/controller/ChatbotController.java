package com.poly.cake.controller;

import com.poly.cake.entity.LichSuChatBot;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.repository.LichSuChatBotRepository;
import com.poly.cake.repository.NguoiDungRepository;
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
    private final LichSuChatBotRepository lichSuChatBotRepository;
    private final NguoiDungRepository nguoiDungRepository;

    @PostMapping(value = "/ask", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> ask(
            @RequestParam("prompt") String prompt,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication authentication) {

        // Nếu khách đã đăng nhập -> dùng email làm sessionId để tách lịch sử hội thoại
        // theo từng người; khách vãng lai dùng chung 1 phiên đơn giản.
        String sessionId = authentication != null ? authentication.getName() : "guest";

        Map<String, String> result = chatbotService.ask(prompt, sessionId, file);

        // Lưu lại lượt hỏi-đáp vào DB (không chặn phản hồi cho khách nếu lưu lỗi)
        try {
            NguoiDung khachHang = authentication != null
                    ? nguoiDungRepository.findByEmail(authentication.getName()).orElse(null)
                    : null;

            LichSuChatBot record = LichSuChatBot.builder()
                    .khachHang(khachHang)
                    .sessionId(sessionId)
                    .cauHoi(prompt)
                    .traLoi(result.getOrDefault("response", ""))
                    .build();
            lichSuChatBotRepository.save(record);
        } catch (Exception ignored) {
            // Lưu lịch sử là phụ — lỗi ở đây không nên làm hỏng trải nghiệm chat của khách
        }

        return ResponseEntity.ok(result);
    }
}
