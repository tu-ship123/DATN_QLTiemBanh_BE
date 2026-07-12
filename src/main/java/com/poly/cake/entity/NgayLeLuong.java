package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * T102 – Cấu hình ngày lễ tính lương.
 * Mỗi bản ghi = 1 ngày lễ cụ thể + hệ số nhân lương áp dụng cho ngày đó
 * (VD: 1/1 Dương lịch heSoLuong = 2.0 -> nhân đôi lương; Tết Nguyên Đán = 3.0).
 */
@Entity
@Table(name = "ngay_le_luong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NgayLeLuong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate ngayLe;

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String tenNgayLe;

    /** Hệ số nhân lương cho ngày này: 1.0 = bình thường, 2.0 = x2, 3.0 = x3... */
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal heSoLuong;

    @Builder.Default
    @Column(nullable = false)
    private Boolean hoatDong = true;
}