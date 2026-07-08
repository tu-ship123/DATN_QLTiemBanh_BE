package com.poly.cake.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopSanPhamDto {
    private String tenSanPham;
    private Long tongSoLuongBan; // Lệnh SUM() trong Spring Data JPA mặc định trả về kiểu Long
}