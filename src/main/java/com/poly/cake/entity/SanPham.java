package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "san_pham")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "danh_muc_id")
    private DanhMuc danhMuc;

    @Column(nullable = false, length = 200)
    private String tenSanPham;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal donGia;

    private Integer soLuongTon = 0;

    @Column(length = 500)
    private String anhSanPham;

    @Column(nullable = false)
    private String trangThai = "DANG_BAN"; // DANG_BAN, TAM_AN

    @Column(columnDefinition = "TEXT")
    private String moTa;

    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}
