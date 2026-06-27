package com.poly.cake.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MaGiamGiaDto {

    @Data
    public static class Request {

        @NotBlank(message = "Mã giảm giá không được để trống")
        @Size(max = 50, message = "Mã giảm giá tối đa 50 ký tự")
        private String maCode;

        @NotBlank(message = "Loại giảm giá không được để trống")
        @Pattern(
                regexp = "PHAN_TRAM|SO_TIEN_CO_DINH",
                message = "Loại giảm giá chỉ được là PHAN_TRAM hoặc SO_TIEN_CO_DINH"
        )
        private String loaiGiamGia;

        @NotNull(message = "Giá trị giảm không được để trống")
        @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
        private BigDecimal giaTriGiam;

        @DecimalMin(value = "0.0", message = "Đơn hàng tối thiểu không hợp lệ")
        private BigDecimal donHangToiThieu;

        @Min(value = 1, message = "Số lượt tối đa phải >= 1")
        private Integer soLuotToiDa;

        @NotNull(message = "Ngày hết hạn không được để trống")
        private LocalDateTime ngayHetHan;

        private Boolean hoatDong = true;

        /** Số điểm cần dùng để đổi mã này (null = không cho đổi bằng điểm) */
        @Min(value = 1, message = "Số điểm cần dùng phải >= 1")
        private Integer diemCanDung;
    }

    @Data
    public static class Response {

        private Long id;
        private String maCode;
        private String loaiGiamGia;
        private BigDecimal giaTriGiam;
        private BigDecimal donHangToiThieu;
        private Integer soLuotToiDa;
        private Integer soLuotDaDung;
        private LocalDateTime ngayHetHan;
        private Boolean hoatDong;
        /** Số điểm cần dùng để đổi (null = không cho đổi bằng điểm) */
        private Integer diemCanDung;
    }
}