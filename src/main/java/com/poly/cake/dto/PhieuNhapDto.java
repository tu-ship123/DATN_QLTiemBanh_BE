package com.poly.cake.dto;

import java.util.List;
import lombok.Data;

@Data
public class PhieuNhapDto {
    private String ghiChu;
    private List<ChiTietNhapDto> chiTietList;

    @Data
    public static class ChiTietNhapDto {
        private Long sanPhamId;
        private Integer soLuong;
        private Double giaNhap;
    }
}