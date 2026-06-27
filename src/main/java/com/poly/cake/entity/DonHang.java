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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
public class DonHang {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrangThaiDonHang trangThai = TrangThaiDonHang.CHO_XAC_NHAN;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include // Thêm dòng này vào ngay trên private Long id;
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private NguoiDung khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_id")
    private NguoiDung nhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_giam_gia_id")
    private MaGiamGia maGiamGia;

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


    @PreUpdate
    protected void onUpdate() {
        ngayCapNhat = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        // Chốt chặn 1: Nếu bằng một cách nào đó bị set null, tự động ép về mặc định
        if (this.trangThai == null) {
            this.trangThai = TrangThaiDonHang.CHO_XAC_NHAN;
        }

        // Chốt chặn 2: Tiện thể tự động gán luôn thời gian tạo đơn nếu chưa có
        if (this.ngayTao == null) {
            this.ngayTao = LocalDateTime.now();
        }
    }
}