package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "phieu_nhap_kho")
@Getter 
@Setter
@NoArgsConstructor 
@AllArgsConstructor
public class PhieuNhapKho {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nguoi_tao_id", nullable = false)
    private Long nguoiTaoId;

    @Column(name = "nguoi_duyet_id")
    private Long nguoiDuyetId;

    @Column(name = "trang_thai", nullable = false)
    private String trangThai = "CHO_DUYET"; // CHO_DUYET, DA_DUYET, DA_HUY

    @Column(name = "tong_tien")
    private Double tongTien;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "ngay_duyet")
    private LocalDateTime ngayDuyet;

    @OneToMany(mappedBy = "phieuNhapKho", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChiTietPhieuNhap> chiTietList;
}