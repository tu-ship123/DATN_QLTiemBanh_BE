package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gio_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GioHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id", nullable = false, unique = true)
    private NguoiDung khachHang;

    // Mã giảm giá khách đã áp dụng ở giỏ hàng, sẽ được mang qua khi checkout
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_giam_gia_id")
    private MaGiamGia maGiamGia;

    // Voucher cá nhân (đổi bằng điểm) khách đã áp dụng ở giỏ hàng.
    // Chỉ 1 trong 2 (maGiamGia HOẶC voucherKhachHang) được áp dụng cùng lúc.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_khach_hang_id")
    private VoucherKhachHang voucherKhachHang;

    @Column(nullable = false)
    private LocalDateTime ngayTao;

    @Column(nullable = false)
    private LocalDateTime ngayCapNhat;

    @OneToMany(mappedBy = "gioHang", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChiTietGioHang> chiTietGioHangs = new ArrayList<>();

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