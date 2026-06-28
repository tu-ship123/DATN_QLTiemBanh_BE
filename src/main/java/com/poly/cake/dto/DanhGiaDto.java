package com.poly.cake.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class DanhGiaDto {

    /** FE gửi lên khi khách đánh giá */
    @Data
    public static class Request {

        @NotNull(message = "Sản phẩm không được để trống")
        private Long sanPhamId;

        @NotNull(message = "Số sao không được để trống")
        @Min(value = 1, message = "Số sao tối thiểu là 1")
        @Max(value = 5, message = "Số sao tối đa là 5")
        private Integer soSao;

        @Size(max = 1000, message = "Nội dung đánh giá tối đa 1000 ký tự")
        private String noiDung;
    }

    /** Response trả về 1 đánh giá */
    @Data
    public static class Response {
        private Long id;
        private Long donHangId;
        private Long sanPhamId;
        private String tenSanPham;
        private String anhSanPham;
        private String tenKhachHang;
        private Integer soSao;
        private String noiDung;
        private String phanHoiCuaTiem;
        private Boolean biAn;
        private LocalDateTime ngayTao;
    }

    /** Trả về danh sách đánh giá của 1 đơn hàng */
    @Data
    public static class DonHangDanhGiaResponse {
        private Long donHangId;
        private boolean daDanhGia;
        private List<Response> danhSach;
    }

    /** ✅ THÊM MỚI: Thống kê tổng quan cho admin */
    @Data
    public static class StatsResponse {
        private long tong;
        private long chuaTraLoi;
        private long biAn;
        private double trungBinhSao;
        private long sao5;
        private long sao4;
        private long sao3;
        private long sao2;
        private long sao1;
    }
}