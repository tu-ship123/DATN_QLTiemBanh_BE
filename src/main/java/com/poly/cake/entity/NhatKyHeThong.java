package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "nhat_ky_he_thong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhatKyHeThong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id")
    private NguoiDung nguoiDung; // Có thể null nếu hệ thống tự chạy

    @Column(nullable = false, length = 100)
    private String hanhDong;

    @Column(length = 100)
    private String tenBang;

    private Long banGhiId;

    @Column(columnDefinition = "TEXT")
    private String giaTriCu;

    @Column(columnDefinition = "TEXT")
    private String giaTriMoi;

    // [ĐÃ SỬA]: Đổi sang nullable = true để tránh lỗi SQL Server khi bảng cũ đã có dữ liệu
    @Column(name = "ngay_tao", nullable = true) 
    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}