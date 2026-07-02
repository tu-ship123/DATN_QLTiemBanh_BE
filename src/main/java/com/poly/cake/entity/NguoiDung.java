package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "nguoi_dung")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NguoiDung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "NVARCHAR(150)")
    private String hoTen;

    @Column(nullable = false, unique = true, columnDefinition = "NVARCHAR(150)")
    private String email;

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String matKhau;

    @Column(columnDefinition = "NVARCHAR(20)")
    private String soDienThoai;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String anhDaiDien;

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String quyen = "KHACH_HANG"; // ADMIN, NHAN_VIEN, KHACH_HANG

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String trangThai = "HOAT_DONG"; // HOAT_DONG, BI_KHOA, NGUNG_HOAT_DONG

    @Column(columnDefinition = "NVARCHAR(10)")
    private String maOtp;

    private LocalDateTime otpHetHan;

    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}