package com.poly.cake.dto;

import jakarta.validation.constraints.Min;
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

    // ─── RESPONSE: Toàn bộ giỏ hàng ─────────────────────────────────────────
    @Data
    public static class GioHangResponse {
        private Long id;
        private List<ChiTietGioHangResponse> items;
        private Integer tongSoLuong;
        private BigDecimal tongTienHang;
        private BigDecimal phiShip;
        private BigDecimal tongThanhToan;
        private LocalDateTime ngayCapNhat;
    }
}
