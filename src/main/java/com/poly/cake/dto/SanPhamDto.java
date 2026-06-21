package com.poly.cake.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SanPhamDto {

    @Data
    public static class Request {

        private Long danhMucId;

        private String tenSanPham;

        private BigDecimal donGia;

        private Integer soLuongTon;

        private String anhSanPham;

        private String trangThai;

        private String moTa;
    }

    @Data
    public static class Response {

        private Long id;

        private Long danhMucId;

        private String tenDanhMuc;

        private String tenSanPham;

        private BigDecimal donGia;

        private Integer soLuongTon;

        private String anhSanPham;

        private String trangThai;

        private String moTa;

        private LocalDateTime ngayTao;
    }
}