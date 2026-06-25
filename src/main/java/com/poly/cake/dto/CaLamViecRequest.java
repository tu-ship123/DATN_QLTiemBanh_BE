package com.poly.cake.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalTime;

/**
 * DTO request để tạo hoặc cập nhật ca làm việc.
 */
@Data
public class CaLamViecRequest {

    @NotBlank(message = "Tên ca không được để trống")
    private String tenCa;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime gioBatDau;

    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime gioKetThuc;

    private Boolean hoatDong = true;
}
