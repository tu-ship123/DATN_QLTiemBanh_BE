package com.poly.cake.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HieuSuatNhanVienDto {
    private Long nhanVienId;
    private String tenNhanVien;
    private Long tongSoDon;
    private BigDecimal tongDoanhThu;
}