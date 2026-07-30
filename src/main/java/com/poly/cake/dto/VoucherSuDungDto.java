package com.poly.cake.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherSuDungDto {
    private String maCode;
    private Long khachHangId;
    private String tenKhachHang;
    private Long donHangId;
    private LocalDateTime thoiDiemSuDung; // Lấy ngày tạo của đơn hàng
}