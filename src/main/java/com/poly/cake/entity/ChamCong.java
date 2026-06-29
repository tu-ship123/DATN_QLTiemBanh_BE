package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bảng chấm công – mỗi bản ghi = 1 lần check-in của nhân viên vào ca.
 *
 * T062: Kết ca X/Z Report được ghi thẳng vào bản ghi chấm công này
 * (không cần bảng riêng). Khi nhân viên kết ca chính thức (Z_REPORT),
 * các cột doanh thu bên dưới được lấp đầy và PhanCa → DA_KET_CA.
 */
@Entity
@Table(name = "cham_cong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChamCong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phan_ca_id", nullable = false, unique = true)
    private PhanCa phanCa;

    private LocalDateTime gioVao;

    private LocalDateTime gioRa;

    private Integer phutDiTre = 0;

    @Column(nullable = false)
    private String trangThai = "DUNG_GIO"; // DUNG_GIO, DI_TRE, VANG_MAT, VE_SOM

    // ── T062: Dữ liệu kết ca (X-Report / Z-Report) ──────────────────────────

    /** Thời điểm nhân viên nhấn kết ca (null nếu chưa kết). */
    private LocalDateTime thoiDiemKetCa;

    /** X_REPORT | Z_REPORT | null (chưa kết ca). */
    @Column(length = 20)
    private String loaiBaoCao;

    /** Tổng số đơn HOAN_THANH thanh toán trong ca. */
    private Integer tongSoDon;

    /** Doanh thu tiền mặt (TIEN_MAT + THANH_CONG) trong ca. */
    @Column(precision = 14, scale = 2)
    private BigDecimal doanhThuTienMat;

    /** Doanh thu SePay – chuyển khoản (CHUYEN_KHOAN + THANH_CONG) trong ca. */
    @Column(precision = 14, scale = 2)
    private BigDecimal doanhThuSepay;

    /** Doanh thu hình thức khác (VNPAY, MOMO…) trong ca. */
    @Column(precision = 14, scale = 2)
    private BigDecimal doanhThuKhac;

    /** Tổng doanh thu toàn ca = tiền mặt + SePay + khác. */
    @Column(precision = 14, scale = 2)
    private BigDecimal tongDoanhThu;

    /** Ghi chú của nhân viên khi kết ca. */
    @Column(columnDefinition = "TEXT")
    private String ghiChuKetCa;

    // ────────────────────────────────────────────────────────────────────────

    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}