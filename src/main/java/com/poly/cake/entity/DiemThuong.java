package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


/**
 * Bảng lưu lịch sử giao dịch điểm thưởng của từng khách hàng.
 * Mỗi hành động (mua hàng, đánh giá, cộng thủ công) tạo ra 1 bản ghi.
 */
@Entity
@Table(name = "diem_thuong", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"khach_hang_id", "don_hang_id", "loai_giao_dich"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiemThuong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private NguoiDung khachHang;

    /** Số điểm thay đổi: dương = cộng, âm = trừ (khi đổi voucher) */
    @Column(nullable = false)
    private Integer diemThayDoi;

    /**
     * Loại giao dịch:
     * MUAT_HANG_ONLINE  – xác nhận đã nhận đơn online
     * DANH_GIA          – đánh giá sản phẩm sau khi nhận hàng
     * MUAT_HANG_POS     – nhân viên quét SĐT khách khi bán tại quầy
     * DOI_VOUCHER       – trừ điểm khi đổi voucher
     * ADMIN_CHINH_SUA   – admin chỉnh tay
     */
    @Column(nullable = false, length = 30)
    private String loaiGiaoDich;

    /** Mô tả nhanh cho từng giao dịch, hiển thị trên UI */
    @Column(length = 300)
    private String moTa;

    /** Liên kết đơn hàng nếu có (có thể null với giao dịch ADMIN) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_id")
    private DonHang donHang;

    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}
