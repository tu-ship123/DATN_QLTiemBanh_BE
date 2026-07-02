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

    /**
     * Mã giảm giá gốc (trong bảng ma_giam_gia) mà voucher này được đổi ra từ điểm.
     * NULL nếu voucher được hệ thống/admin tặng trực tiếp, không qua đổi điểm từ 1 mã cụ thể.
     * Dùng để khi khách DÙNG voucher cá nhân này ở đơn hàng, hệ thống cộng dồn lượt sử dụng
     * ngược lại về mã gốc — để trang quản lý voucher (đọc từ bảng ma_giam_gia) thống kê đúng.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_giam_gia_goc_id")
    private MaGiamGia maGiamGiaGoc;

    /** Tên hiển thị cho khách: "Giảm 50k đơn từ 200k", ... */
    @Column(nullable = false, columnDefinition = "NVARCHAR(200)")
    private String tenVoucher;

    /** PHAN_TRAM | SO_TIEN_CO_DINH */
    @Column(nullable = false, columnDefinition = "NVARCHAR(20)")
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
    @Column(nullable = false, columnDefinition = "NVARCHAR(20)")
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
