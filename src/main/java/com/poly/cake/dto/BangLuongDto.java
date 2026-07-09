package com.poly.cake.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BangLuongDto {
    private Long nhanVienId;
    private String tenNhanVien;
    private double tongGioLam;
    private int tongPhutTre;
    private BigDecimal luongCoBan;
    private BigDecimal tienPhat;
    private BigDecimal luongThucLanh;
}