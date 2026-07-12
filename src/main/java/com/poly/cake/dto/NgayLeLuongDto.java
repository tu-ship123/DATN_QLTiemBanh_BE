package com.poly.cake.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NgayLeLuongDto {

    @Data
    public static class Request {

        @NotNull(message = "Ngày lễ không được để trống")
        private LocalDate ngayLe;

        @NotBlank(message = "Tên ngày lễ không được để trống")
        private String tenNgayLe;

        @NotNull(message = "Hệ số lương không được để trống")
        @DecimalMin(value = "1.0", message = "Hệ số lương phải >= 1.0 (VD: 2.0 = x2, 3.0 = x3)")
        private BigDecimal heSoLuong;

        private Boolean hoatDong = true;
    }

    @Data
    public static class Response {
        private Long id;
        private LocalDate ngayLe;
        private String tenNgayLe;
        private BigDecimal heSoLuong;
        private Boolean hoatDong;
    }
}