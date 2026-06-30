package com.poly.cake.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OrderProcessDto {
    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(
            regexp = "DA_XAC_NHAN|DANG_LAM|SAN_SANG|DANG_GIAO|HOAN_THANH|DA_HUY",
            message = "Trạng thái không hợp lệ"
    )
    private String trangThai; // DA_XAC_NHAN, DANG_LAM, SAN_SANG, DANG_GIAO, HOAN_THANH, DA_HUY

    private String lyDoHuy;   // Chỉ bắt buộc gửi lên khi trangThai là DA_HUY
}