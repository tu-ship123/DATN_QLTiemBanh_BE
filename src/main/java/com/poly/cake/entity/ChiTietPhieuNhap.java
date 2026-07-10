package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chi_tiet_phieu_nhap")
@Getter 
@Setter
@NoArgsConstructor 
@AllArgsConstructor
public class ChiTietPhieuNhap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phieu_nhap_id", nullable = false)
    private PhieuNhapKho phieuNhapKho;

    @Column(name = "san_pham_id", nullable = false)
    private Long sanPhamId;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "gia_nhap", nullable = false)
    private Double giaNhap;
}