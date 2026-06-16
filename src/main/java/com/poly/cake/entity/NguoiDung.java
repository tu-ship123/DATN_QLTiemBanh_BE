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

    @Column(nullable = false, length = 150)
    private String hoTen;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String matKhau;

    @Column(length = 20)
    private String soDienThoai;

    @Column(length = 500)
    private String anhDaiDien;

    @Column(nullable = false)
    private String quyen = "KHACH_HANG"; // ADMIN, NHAN_VIEN, KHACH_HANG

    @Column(nullable = false)
    private String trangThai = "HOAT_DONG"; // HOAT_DONG, BI_KHOA, NGUNG_HOAT_DONG

    @Column(length = 10)
    private String maOtp;

    private LocalDateTime otpHetHan;

    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}