package com.poly.cake.service;

import com.poly.cake.dto.ChatbotTomTatHoiThoaiDto;
import com.poly.cake.dto.ChatbotLichSuDto;
import com.poly.cake.entity.LichSuChatBot;
import com.poly.cake.repository.LichSuChatBotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatbotLichSuService {

    private final LichSuChatBotRepository lichSuChatBotRepository;

    // ── ADMIN: danh sách các phiên chat với bot, mới nhất lên đầu ──
    @Transactional(readOnly = true)
    public List<ChatbotTomTatHoiThoaiDto> getConversations() {
        List<String> sessionIds = lichSuChatBotRepository.findDistinctSessionIdsOrderByLatestChat();
        return sessionIds.stream().map(sessionId -> {
            LichSuChatBot latest = lichSuChatBotRepository.findTopBySessionIdOrderByNgayTaoDesc(sessionId);
            long soLuot = lichSuChatBotRepository.countBySessionId(sessionId);
            String tenKhachHang = latest.getKhachHang() != null ? latest.getKhachHang().getHoTen() : null;
            return new ChatbotTomTatHoiThoaiDto(
                    sessionId, tenKhachHang, latest.getCauHoi(), latest.getNgayTao(), soLuot);
        }).collect(Collectors.toList());
    }

    // ── Toàn bộ lượt hỏi-đáp của 1 phiên chat cụ thể ──
    @Transactional(readOnly = true)
    public List<ChatbotLichSuDto> getHistory(String sessionId) {
        return lichSuChatBotRepository.findBySessionIdOrderByNgayTaoAsc(sessionId).stream()
                .map(c -> new ChatbotLichSuDto(c.getId(), c.getCauHoi(), c.getTraLoi(), c.getNgayTao()))
                .collect(Collectors.toList());
    }
}
