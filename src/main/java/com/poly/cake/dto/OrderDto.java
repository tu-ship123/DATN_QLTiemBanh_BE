package com.poly.cake.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {

    // 1. DTO dùng để NHẬN dữ liệu từ Frontend gửi lên (Request)
    @Data
    public static class Request {
        private String diaChiGiaoHang;
        private String soDienThoai;
        private LocalDate ngayGiaoHang;
        private String ghiChu;
        private List<OrderItemRequest> items;
    }

    @Data
    public static class OrderItemRequest {
        private Long sanPhamId;
        private Integer soLuong;
        private Double donGia;
    }

    // 2. DTO dùng để TRẢ dữ liệu về cho Frontend (Response)
    @Data
    public static class Response {
        private Long id;
        private String maDonHang;
        private String diaChiGiaoHang;
        private String soDienThoai;
        private LocalDate ngayGiaoHang;
        private LocalDateTime ngayTao;
        private Double phiShip;
        private Double tongTien;
        private String trangThai;
        private String ghiChu;
        private String emailNguoiDung;
        private List<OrderItemResponse> items;
    }

    @Data
    public static class OrderItemResponse {
        private Long sanPhamId;
        private String tenSanPham;
        private Integer soLuong;
        private Double giaBan;
    }
}