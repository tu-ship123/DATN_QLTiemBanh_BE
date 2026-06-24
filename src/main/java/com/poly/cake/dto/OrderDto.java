package com.poly.cake.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {

    // 1. DTO nhận dữ liệu tạo đơn từ FE (Request)
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

    // 2. DTO trả dữ liệu về FE (Response)
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
        private String tenNhanVienPhuTrach;
        private String lyDoHuy;
        private List<OrderItemResponse> items;
    }

    @Data
    public static class OrderItemResponse {
        private Long sanPhamId;
        private String tenSanPham;
        private Integer soLuong;
        private Double giaBan;
    }

    // ── MỚI 5: DTO chỉnh sửa thông tin đơn ────────────────────────────────────
    @Data
    public static class UpdateRequest {
        private String diaChiGiaoHang;  // Cho phép null = không đổi
        private String soDienThoai;
        private LocalDate ngayGiaoHang;
        private String ghiChu;
    }

    // ── MỚI 7: DTO dữ liệu in đơn ─────────────────────────────────────────────
    @Data
    public static class PrintResponse {
        // Thông tin đơn hàng
        private Long id;
        private String maDonHang;
        private String trangThai;
        private LocalDateTime ngayTao;
        private LocalDate ngayGiaoHang;
        private Double tongTien;
        private Double soTienCoc;
        private Double conLai;      // tongTien - soTienCoc
        private String ghiChu;
        private String nguonDon;

        // Thông tin khách hàng
        private String tenKhachHang;
        private String emailKhachHang;
        private String sdtKhachHang;
        private String diaChiGiaoHang;

        // Nhân viên phụ trách
        private String tenNhanVien;

        // Danh sách sản phẩm
        private List<PrintItem> items;

        @Data
        public static class PrintItem {
            private String tenSanPham;
            private Integer soLuong;
            private Double donGia;
            private Double thanhTien;   // soLuong * donGia
        }
    }
}