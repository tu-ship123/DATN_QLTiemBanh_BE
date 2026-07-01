package com.poly.cake.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class GioHangDto {

    // ─── REQUEST: Thêm sản phẩm vào giỏ ─────────────────────────────────────
    @Data
    public static class ThemVaoGioRequest {
        @NotNull(message = "Sản phẩm không được để trống")
        private Long sanPhamId;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng tối thiểu là 1")
        private Integer soLuong;

        private String thietKeBanhJson; // Dữ liệu thiết kế 3D (nếu có)
    }

    // ─── REQUEST: Cập nhật số lượng ──────────────────────────────────────────
    @Data
    public static class CapNhatSoLuongRequest {
        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng tối thiểu là 1")
        private Integer soLuong;
    }

    // ─── RESPONSE: Thông tin 1 sản phẩm trong giỏ ───────────────────────────
    @Data
    public static class ChiTietGioHangResponse {
        private Long id;             // ID của chi tiết giỏ hàng
        private Long sanPhamId;
        private String tenSanPham;
        private String anhSanPham;
        private String tenDanhMuc;
        private BigDecimal donGia;
        private Integer soLuong;
        private BigDecimal thanhTien;  // donGia * soLuong
        private String thietKeBanhJson;
        private LocalDateTime ngayTao;
    }

    // ─── REQUEST: Áp dụng mã giảm giá vào giỏ hàng ──────────────────────────
    @Data
    public static class ApplyDiscountRequest {
        @NotBlank(message = "Mã giảm giá không được để trống")
        private String maCode;
    }

    // ─── REQUEST: Áp dụng voucher cá nhân (đổi bằng điểm) vào giỏ hàng ──────
    @Data
    public static class ApplyVoucherKhachHangRequest {
        @NotNull(message = "Voucher không được để trống")
        private Long voucherKhachHangId;
    }

    // ─── RESPONSE: Toàn bộ giỏ hàng ─────────────────────────────────────────
    @Data
    public static class GioHangResponse {
        private Long id;
        private List<ChiTietGioHangResponse> items;
        private Integer tongSoLuong;
        private BigDecimal tongTienHang;
        private BigDecimal phiShip;

        // Thông tin mã giảm giá đang áp dụng (null nếu chưa áp mã nào)
        private String maGiamGiaCode;
        private String loaiGiamGia;
        private BigDecimal soTienGiam;

        // Thông tin voucher cá nhân (đổi bằng điểm) đang áp dụng, nếu có.
        // Chỉ 1 trong 2 (mã giảm giá / voucher cá nhân) có giá trị cùng lúc.
        private Long voucherKhachHangId;
        private String tenVoucherKhachHang;

        private BigDecimal tongThanhToan;
        private LocalDateTime ngayCapNhat;
    }
}