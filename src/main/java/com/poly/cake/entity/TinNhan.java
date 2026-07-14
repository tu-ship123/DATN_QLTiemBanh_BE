package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tin nhắn giữa khách hàng và cửa hàng (Admin/CSKH).
 * Đơn giản hóa: gom theo 1 khách hàng = 1 cuộc hội thoại (không hỗ trợ group chat).
 */
@Entity
@Table(name = "tin_nhan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TinNhan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cuộc hội thoại được nhóm theo khách hàng này (dù ai gửi: khách hay admin)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private NguoiDung khachHang;

    // Người thực sự gửi tin nhắn này (khách hàng hoặc nhân viên/admin trả lời)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_gui_id", nullable = false)
    private NguoiDung nguoiGui;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String noiDung;

    // true nếu tin nhắn này do phía cửa hàng (Admin/NhanVien) gửi, false nếu khách gửi
    @Column(nullable = false)
    private boolean tuCuaHang;

    @Column(nullable = false)
    @Builder.Default
    private boolean daDoc = false;

    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}
