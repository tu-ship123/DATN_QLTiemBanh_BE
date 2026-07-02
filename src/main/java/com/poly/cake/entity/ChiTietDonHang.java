package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "chi_tiet_don_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChiTietDonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_id", nullable = false)
    private DonHang donHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    private Integer soLuong = 1;

    @Column(precision = 12, scale = 2)
    private BigDecimal donGiaTaiThoiDiem;

    /**
     * Snapshot JSON thiết kế bánh 3D của riêng item này tại thời điểm đặt hàng
     * (copy nguyên văn từ ChiTietGioHang.thietKeBanhJson lúc checkout), để không bị
     * mất dữ liệu khi giỏ hàng đã bị xóa sau khi tạo đơn thành công.
     */
    @Column(columnDefinition = "TEXT")
    private String thietKeBanhJson;

    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}