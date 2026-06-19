package com.poly.cake.dto;

import lombok.Data;

@Data
public class OrderProcessDto {
    private String trangThai; // DA_XAC_NHAN, DANG_LAM, SAN_SANG, DANG_GIAO, HOAN_THANH, DA_HUY
    private String lyDoHuy;   // Chỉ bắt buộc gửi lên khi trangThai là DA_HUY
}