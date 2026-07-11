package com.poly.cake.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dia_chi_giao_hang")
@Data
public class DiaChiGiaoHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nguoi_dung_id", nullable = false)
    private Long nguoiDungId;

    @Column(name = "ho_ten_nguoi_nhan", nullable = false, length = 150)
    private String hoTenNguoiNhan;

    @Column(name = "so_dien_thoai_nhan", nullable = false, length = 20)
    private String soDienThoaiNhan;

    @Column(name = "dia_chi_chi_tiet", nullable = false, length = 500)
    private String diaChiChiTiet;

    @Column(name = "la_mac_dinh")
    private Boolean laMacDinh = false;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao = LocalDateTime.now();
}