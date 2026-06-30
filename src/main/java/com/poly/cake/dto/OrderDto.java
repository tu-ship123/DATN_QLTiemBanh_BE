package com.poly.cake.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {

    // 1. DTO nhận dữ liệu tạo đơn từ FE (Request)
    @Data
    public static class Request {
        @NotBlank(message = "Địa chỉ giao hàng không được để trống")
        private String diaChiGiaoHang;

        @NotBlank(message = "Số điện thoại không được để trống")
        private String soDienThoai;

        @NotNull(message = "Ngày giao hàng không được để trống")
        private LocalDate ngayGiaoHang;

        private String ghiChu;

        @NotEmpty(message = "Đơn hàng phải có ít nhất 1 sản phẩm")
        @Valid
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
        @NotNull(message = "Sản phẩm không được để trống")
        private Long sanPhamId;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng tối thiểu là 1")
        private Integer soLuong;

        @NotNull(message = "Đơn giá không được để trống")
        @Min(value = 0, message = "Đơn giá không hợp lệ")
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
        @NotBlank(message = "Địa chỉ giao hàng không được để trống")
        private String diaChiGiaoHang;

        @NotBlank(message = "Số điện thoại không được để trống")
        private String soDienThoai;

        @NotNull(message = "Ngày giao hàng không được để trống")
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