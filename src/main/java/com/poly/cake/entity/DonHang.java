package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "don_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private NguoiDung khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_id")
    private NguoiDung nhanVien;

    // Giả sử bạn sẽ có entity MaGiamGia
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "ma_giam_gia_id")
    // private MaGiamGia maGiamGia;

    @Column(nullable = false)
    private String trangThai = "CHO_XAC_NHAN";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tongTien;

    @Column(precision = 12, scale = 2)
    private BigDecimal soTienCoc = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String thietKeBanhJson;

    private String diaChiGiao;

    private LocalDateTime ngayGiaoDuKien;

    @Column(columnDefinition = "TEXT")
    private String ghiChu;

    private LocalDateTime ngayTao;

    private LocalDateTime ngayCapNhat;

    @Column(columnDefinition = "TEXT")
    private String lyDoHuy;

    private LocalDateTime thoiDiemGiao;

    @Column(nullable = false)
    private String nguonDon = "ONLINE"; // ONLINE, POS

    // Mapping 1-Nhiều với bảng chi tiết đơn hàng
    @OneToMany(mappedBy = "donHang", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChiTietDonHang> chiTietDonHangs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        ngayCapNhat = LocalDateTime.now();
    }
}