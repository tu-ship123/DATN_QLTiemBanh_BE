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

        /**
         * T055 – Dữ liệu thiết kế bánh 3D (tuỳ chọn).
         * FE gửi lên dưới dạng chuỗi JSON (stringify của object Three.js).
         * Nếu đơn không có bánh 3D thì để null.
         */
        private String cakeDesignJson;
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

        /** T055 – Có thiết kế 3D không? (true/false để FE hiện nút "Xem 3D") */
        private Boolean coThietKe3D;
    }

    @Data
    public static class OrderItemResponse {
        private Long sanPhamId;
        private String tenSanPham;
        private Integer soLuong;
        private Double giaBan;
    }

    // ── Chỉnh sửa thông tin đơn ──────────────────────────────────────────────
    @Data
    public static class UpdateRequest {
        private String diaChiGiaoHang;
        private String soDienThoai;
        private LocalDate ngayGiaoHang;
        private String ghiChu;
    }

    // ── Dữ liệu in đơn ───────────────────────────────────────────────────────
    @Data
    public static class PrintResponse {
        private Long id;
        private String maDonHang;
        private String trangThai;
        private LocalDateTime ngayTao;
        private LocalDate ngayGiaoHang;
        private Double tongTien;
        private Double soTienCoc;
        private Double conLai;
        private String ghiChu;
        private String nguonDon;

        private String tenKhachHang;
        private String emailKhachHang;
        private String sdtKhachHang;
        private String diaChiGiaoHang;

        private String tenNhanVien;

        private List<PrintItem> items;

        @Data
        public static class PrintItem {
            private String tenSanPham;
            private Integer soLuong;
            private Double donGia;
            private Double thanhTien;
        }
    }
}