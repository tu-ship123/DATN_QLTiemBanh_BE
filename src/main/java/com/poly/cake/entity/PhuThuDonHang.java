package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * T102 – Cấu hình phụ thu cho dịp đặc biệt (Tết, Valentine, Giáng sinh...).
 * Đơn hàng có ngày giao rơi vào khoảng [ngayBatDau, ngayKetThuc] sẽ tự động
 * bị cộng thêm phanTramPhuThu (%) vào tiền hàng.
 */
@Entity
@Table(name = "phu_thu_don_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhuThuDonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String tenDip; // VD: "Tết Nguyên Đán 2027", "Valentine 14/2"

    @Column(nullable = false)
    private LocalDate ngayBatDau;

    @Column(nullable = false)
    private LocalDate ngayKetThuc;

    /** Phần trăm phụ thu, VD: 10 = +10% trên tổng tiền hàng */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal phanTramPhuThu;

    @Builder.Default
    @Column(nullable = false)
    private Boolean hoatDong = true;
}