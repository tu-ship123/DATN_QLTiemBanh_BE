package com.poly.cake.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO cho trang "Hiệu suất nhân viên" (Admin) — GET /api/v1/admin/staff/hieu-suat
 * Field name khớp với những gì FE (StaffPerformancePage.vue) đang đọc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NhanVienHieuSuatDto {
    private Long id;
    private String hoTen;
    private String email;
    private Boolean hoatDong;
    private Long soLuongDonXuLy;
    private BigDecimal doanhThu;
    private Integer hieuSuat; // % so với nhân viên có doanh thu cao nhất trong khoảng thời gian
}
