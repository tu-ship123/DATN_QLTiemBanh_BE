package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "chi_tiet_gio_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChiTietGioHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gio_hang_id", nullable = false)
    private GioHang gioHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    private Integer soLuong = 0;

    @Column(columnDefinition = "TEXT")
    private String thietKeBanhJson;

    /**
     * Giá đã tính riêng cho item này khi có thiết kế 3D tùy chỉnh (size + số tầng +
     * phụ kiện trang trí khách tự chọn ở CakeBuilder3D). Khi null, giá dùng
     * sanPham.donGia như bình thường (sản phẩm bán sẵn, không tùy chỉnh).
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal donGiaTuyChinh;

    @Column(nullable = false)
    private LocalDateTime ngayTao;

    @Column(nullable = false)
    private LocalDateTime ngayCapNhat;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
        ngayCapNhat = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        ngayCapNhat = LocalDateTime.now();
    }
}