package com.poly.cake.dto;

import lombok.Data;

import java.math.BigDecimal;

public class PhuKienTrangTriDto {

    /**
     * T050 - DTO trả về cho khách hàng xem danh sách phụ kiện trang trí còn hàng.
     */
    @Data
    public static class Response {

        private Long id;

        private String tenPhuKien;

        private BigDecimal donGia;

        private Integer soLuongTon;

        private String anhPhuKien;
    }
}
