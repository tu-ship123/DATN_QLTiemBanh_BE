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

    // Hậu tố email nội bộ (giả) cấp cho tài khoản đăng ký qua OTP SĐT, vì
    // cột email là NOT NULL UNIQUE. Dùng chung giữa AuthService (khi tạo)
    // và HoSoService (khi cần ẩn đi, không hiển thị cho người dùng thấy).
    public static final String PHONE_EMAIL_SUFFIX = "@phone.chocopine.local";

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

    // Liên kết tài khoản Google (Google "sub" - định danh duy nhất, không đổi theo email)
    // Null với tài khoản đăng ký thường (email/mật khẩu hoặc OTP SĐT)
    @Column(name = "google_id", unique = true, columnDefinition = "NVARCHAR(255)")
    private String googleId;

    private LocalDateTime ngayTao;

    @Column(name = "totp_secret", columnDefinition = "NVARCHAR(100)")
    private String totpSecret;

    @Column(name = "is_2fa_enabled", nullable = false)
    private Boolean is2FaEnabled = false;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}