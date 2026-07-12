package com.poly.cake.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PhuThuDonHangDto {

    @Data
    public static class Request {

        @NotBlank(message = "Tên dịp không được để trống")
        private String tenDip;

        @NotNull(message = "Ngày bắt đầu không được để trống")
        private LocalDate ngayBatDau;

        @NotNull(message = "Ngày kết thúc không được để trống")
        private LocalDate ngayKetThuc;

        @NotNull(message = "Phần trăm phụ thu không được để trống")
        @DecimalMin(value = "0.01", message = "Phần trăm phụ thu phải lớn hơn 0")
        @DecimalMax(value = "100.0", message = "Phần trăm phụ thu không được vượt quá 100%")
        private BigDecimal phanTramPhuThu;

        private Boolean hoatDong = true;
    }

    @Data
    public static class Response {
        private Long id;
        private String tenDip;
        private LocalDate ngayBatDau;
        private LocalDate ngayKetThuc;
        private BigDecimal phanTramPhuThu;
        private Boolean hoatDong;
    }
}