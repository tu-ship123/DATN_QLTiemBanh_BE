package com.poly.cake.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * T070 – POST /api/v1/vouchers/validate
 *
 * API dùng chung cho cả khách vãng lai (guest) lẫn khách đã đăng nhập để
 * kiểm tra TOÀN BỘ điều kiện của 1 mã giảm giá (hoặc voucher cá nhân) TRƯỚC
 * khi đặt hàng, mà KHÔNG cần phải có giỏ hàng lưu sẵn trên server (guest
 * không có giỏ hàng persist theo tài khoản).
 *
 * FE tự tính tongTienHang từ danh sách sản phẩm trong giỏ (đơn giá * số lượng),
 * gửi lên cùng mã cần kiểm tra để BE trả về có hợp lệ hay không + số tiền
 * được giảm thực tế, tránh phải tạo đơn hàng "thử" rồi mới biết.
 */
public class VoucherKiemTraDto {

    @Data
    public static class Request {

        /**
         * Mã giảm giá (public code, vd "SALE50"). Bắt buộc phải có ĐÚNG 1 trong 2:
         * maCode HOẶC voucherKhachHangId.
         */
        private String maCode;

        /**
         * ID voucher cá nhân (đổi bằng điểm) muốn kiểm tra. Chỉ hợp lệ khi
         * người gọi API đã đăng nhập (voucher gắn với tài khoản) — khách vãng
         * lai gửi trường này sẽ bị từ chối.
         */
        private Long voucherKhachHangId;

        /** Tổng tiền hàng (chưa gồm phí ship, chưa trừ giảm giá) để đối chiếu điều kiện đơn tối thiểu */
        @NotNull(message = "Tổng tiền hàng không được để trống")
        @DecimalMin(value = "0.0", inclusive = true, message = "Tổng tiền hàng không hợp lệ")
        private BigDecimal tongTienHang;
    }

    @Data
    public static class Response {
        /** true nếu mã/voucher hợp lệ và có thể áp dụng ngay */
        private boolean hopLe;

        /** Loại ưu đãi: MA_GIAM_GIA (public code) | VOUCHER_CA_NHAN (đổi điểm) */
        private String loaiUuDai;

        private String maCode;
        private String tenVoucher;
        private String loaiGiamGia; // PHAN_TRAM | SO_TIEN_CO_DINH
        private BigDecimal giaTriGiam;
        private BigDecimal donHangToiThieu;

        /** Số tiền thực tế được giảm dựa trên tongTienHang gửi lên (đã kẹp không vượt quá tổng tiền hàng) */
        private BigDecimal soTienGiam;

        /** Tổng tiền hàng sau khi trừ giảm giá (chưa cộng phí ship) */
        private BigDecimal tongTienSauGiam;

        /** Thông báo mô tả kết quả (lý do không hợp lệ nếu hopLe = false) */
        private String message;
    }
}
