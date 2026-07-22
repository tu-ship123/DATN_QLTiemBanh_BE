package com.poly.cake.repository;

import com.poly.cake.entity.LichSuChatBot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuChatBotRepository extends JpaRepository<LichSuChatBot, Long> {

    List<LichSuChatBot> findByKhachHangIdOrderByNgayTaoAsc(Long khachHangId);

    List<LichSuChatBot> findBySessionIdOrderByNgayTaoAsc(String sessionId);

    // Danh sách các phiên chat (sessionId), mới nhất lên đầu -> dùng cho "Hộp thư" admin
    @Query("SELECT c.sessionId FROM LichSuChatBot c GROUP BY c.sessionId ORDER BY MAX(c.ngayTao) DESC")
    List<String> findDistinctSessionIdsOrderByLatestChat();

    LichSuChatBot findTopBySessionIdOrderByNgayTaoDesc(String sessionId);

    long countBySessionId(String sessionId);
}
