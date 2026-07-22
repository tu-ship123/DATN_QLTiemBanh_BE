package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lịch sử hỏi-đáp với trợ lý ảo AI (khác với TinNhan — vốn là nhắn tin thật
 * với nhân viên). khachHang có thể NULL vì khách vãng lai (chưa đăng nhập)
 * vẫn dùng được chatbot; khi đó dựa vào sessionId để nhóm hội thoại.
 */
@Entity
@Table(name = "lich_su_chatbot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichSuChatBot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id", nullable = true)
    private NguoiDung khachHang;

    // Dùng để nhóm hội thoại khi khách vãng lai (chưa đăng nhập) — vd "guest"
    // hoặc 1 id phiên trình duyệt tự sinh ở FE.
    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String cauHoi;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String traLoi;

    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}
