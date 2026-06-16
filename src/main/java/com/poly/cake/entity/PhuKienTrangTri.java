package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "phu_kien_trang_tri")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhuKienTrangTri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String tenPhuKien;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal donGia;

    private Integer soLuongTon = 0;

    @Column(length = 500)
    private String anhPhuKien;

    private Boolean hoatDong = true;

    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}