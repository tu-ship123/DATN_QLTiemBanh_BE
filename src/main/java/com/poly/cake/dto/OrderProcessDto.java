package com.poly.cake.dto;

import com.poly.cake.entity.TrangThaiDonHang;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data // BẮT BUỘC phải có cái này để IntelliJ tự sinh hàm getTrangThai()
public class OrderProcessDto {

    @NotNull(message = "Trạng thái không được để trống")
    private TrangThaiDonHang trangThai; // Dùng trực tiếp Enum luôn

    private String lyDoHuy;
}