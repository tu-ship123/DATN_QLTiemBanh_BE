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

public class DatHangDto {

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

        /**
         * FIX: Hình thức thanh toán khách chọn ở màn Checkout ("COD" hoặc "SEPAY").
         * Trước đây field này không tồn tại nên BE không biết đơn được thanh toán
         * kiểu gì, dẫn tới không tạo được bản ghi ThanhToan tương ứng khi tạo đơn
         * (xem createOrder() trong DatHangService). Mặc định "COD" nếu FE không gửi,
         * để tương thích ngược với client cũ.
         */
        private String phuongThucThanhToan = "COD";

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

        /**
         * Snapshot JSON thiết kế bánh 3D của item này (copy từ giỏ hàng sang lúc
         * checkout), để nhân viên bếp xem lại qua GET /api/v1/orders/{id}/design.
         * Optional - null nếu là sản phẩm bán sẵn thông thường.
         */
        private String thietKeBanhJson;
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
        private Double soTienPhuThu;

        private Double tongTien;
        private String trangThai;
        private String ghiChu;

        // T080 – Ghi chú nội bộ (chỉ nhân viên/bếp thấy). Service chỉ set giá trị
        // này khi người gọi API là ADMIN/NHAN_VIEN; luôn để null với khách hàng.
        private String ghiChuNoiBo;
        private String emailNguoiDung;
        private String tenNhanVienPhuTrach;
        private String lyDoHuy;
        private List<OrderItemResponse> items;

        // Thông tin mã giảm giá đã áp dụng cho đơn này (null nếu không dùng mã)
        private String maGiamGiaCode;
        private Double soTienGiam;

        // Tên voucher cá nhân đã áp dụng cho đơn này (null nếu dùng mã giảm giá hoặc không dùng gì)
        private String tenVoucherKhachHang;

        /** T055 – Có thiết kế 3D không? (true/false để FE hiện nút "Xem 3D") */
        private Boolean coThietKe3D;

        // DF_ST06 – Trạng thái yêu cầu sửa đơn gần nhất của khách (nếu có):
        // CHO_XU_LY / DA_XU_LY / null (chưa từng gửi yêu cầu nào)
        private String trangThaiYeuCauSuaDon;
    }

    // ── DF_ST05: "Đặt lại đơn cũ" (Re-order) ─────────────────────────────────
    // Trả về giỏ hàng sau khi đã copy TOÀN BỘ sản phẩm của đơn cũ vào giỏ,
    // kèm danh sách sản phẩm không thể thêm lại được (nếu có) để FE báo cho khách.
    @Data
    public static class ReorderResponse {
        private GioHangDto.GioHangResponse gioHang;
        private Integer soSanPhamDaThem;
        private List<String> sanPhamBiBoQua;
    }

    @Data
    public static class OrderItemResponse {
        private Long sanPhamId;
        private String tenSanPham;
        private Integer soLuong;
        private Double giaBan;
        private String thietKeBanhJson;
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

        // T080 – Ghi chú nội bộ hiển thị trên phiếu in cho Bếp/Nhân viên
        private String ghiChuNoiBo;
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