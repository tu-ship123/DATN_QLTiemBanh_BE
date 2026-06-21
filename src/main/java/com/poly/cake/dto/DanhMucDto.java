package com.poly.cake.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class DanhMucDto {

    @Data
public static class Request {
    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 150)
    private String tenDanhMuc;
    private String moTa;
    private String anhDaiDien;
    private Boolean hoatDong;
}

    @Data
    public static class Response {
        private Long id;
        private String tenDanhMuc;
        private String moTa;
        private String anhDaiDien;
        private Boolean hoatDong;
    }
}