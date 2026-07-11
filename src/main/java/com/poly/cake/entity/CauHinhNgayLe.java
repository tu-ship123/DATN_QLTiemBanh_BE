package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "cau_hinh_ngay_le")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CauHinhNgayLe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_ngay_le", nullable = false)
    private String tenNgayLe; // VD: Tết Nguyên Đán

    @Column(name = "ngay", unique = true, nullable = false)
    private LocalDate ngay; // Ngày cụ thể của năm đó

    @Column(name = "he_so_luong")
    private Double heSoLuong; // VD: 2.0 (nhân đôi) hoặc 3.0 (nhân ba)

    @Column(name = "phan_tram_phu_thu")
    private Double phanTramPhuThu; // VD: 10.0 (tương đương phụ thu thêm 10%)
}