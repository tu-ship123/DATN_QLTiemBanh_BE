package com.poly.cake.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoiSoatDto {
    private Long donHangId;
    private String maGiaoDich;
    private String hinhThuc;
    private BigDecimal soTien;
    private String trangThai;
    private LocalDateTime thoiDiemThanhToan;
}