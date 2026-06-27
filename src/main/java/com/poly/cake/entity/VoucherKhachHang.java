package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Voucher khuyến nghị cá nhân – chỉ dành cho từng khách hàng cụ thể.
 * Khác với MaGiamGia (code public dùng chung), đây là voucher
 * được hệ thống tặng tự động hoặc admin phát cho từng người.
 *
 * Cách tích hợp:
 *   • Khi đổi điểm → tạo bản ghi này, ghi diemSuDung vào DiemThuong (-).
 *   • Khi checkout  → FE gửi voucherKhachHangId, BE kiểm tra và áp dụng.
 */
@Entity
@Table(name = "voucher_khach_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherKhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private NguoiDung khachHang;

    /** Tên hiển thị cho khách: "Giảm 50k đơn từ 200k", ... */
    @Column(nullable = false, length = 200)
    private String tenVoucher;

    /** PHAN_TRAM | SO_TIEN_CO_DINH */
    @Column(nullable = false, length = 20)
    private String loaiGiam;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal giaTriGiam;

    @Column(precision = 12, scale = 2)
    private BigDecimal donHangToiThieu;

    /** Số điểm khách đã dùng để đổi voucher này (null = voucher tặng) */
    private Integer diemSuDung;

    /**
     * Trạng thái:
     * CHUA_SU_DUNG | DA_SU_DUNG | HET_HAN | DA_HUY
     */
    @Column(nullable = false, length = 20)
    private String trangThai = "CHUA_SU_DUNG";

    @Column(nullable = false)
    private LocalDateTime ngayHetHan;

    private LocalDateTime ngayTao;
    private LocalDateTime ngaySuDung;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}
