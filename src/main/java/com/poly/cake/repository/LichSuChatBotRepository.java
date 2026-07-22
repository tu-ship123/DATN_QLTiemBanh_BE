package com.poly.cake.repository;

import com.poly.cake.entity.LichSuChatBot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuChatBotRepository extends JpaRepository<LichSuChatBot, Long> {

    List<LichSuChatBot> findByKhachHangIdOrderByNgayTaoAsc(Long khachHangId);

    List<LichSuChatBot> findBySessionIdOrderByNgayTaoAsc(String sessionId);
}
