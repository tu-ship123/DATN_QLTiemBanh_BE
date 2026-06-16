package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "thanh_toan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_id", nullable = false, unique = true)
    private DonHang donHang;

    @Column(nullable = false)
    private String hinhThuc; // VNPAY, MOMO, TIEN_MAT, CHUYEN_KHOAN

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal soTien;

    private String maGiaoDich;

    @Column(nullable = false)
    private String trangThai = "CHO_THANH_TOAN"; // CHO_THANH_TOAN, THANH_CONG, THAT_BAI, DA_HOAN_TIEN

    private LocalDateTime thoiDiemThanhToan;

    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}
