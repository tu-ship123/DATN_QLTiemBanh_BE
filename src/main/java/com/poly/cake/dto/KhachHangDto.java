package com.poly.cake.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

public class KhachHangDto {

    /** Thông tin khách hàng trả về cho admin */
    @Data
    public static class Response {
        private Long id;
        private String hoTen;
        private String email;
        private String soDienThoai;
        private String anhDaiDien;
        private String trangThai;
        private LocalDateTime ngayTao;

        // Thống kê tổng hợp
        private Integer tongDiem;          // tổng điểm thưởng hiện có
        private Long tongDonHang;          // tổng số đơn đã đặt
        private String tongChiTieu;        // tổng tiền đã chi (format VN)
    }

    /** Admin cập nhật thông tin cơ bản của khách */
    @Data
    public static class UpdateRequest {
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150)
        private String hoTen;

        @Size(max = 20)
        private String soDienThoai;

        @NotBlank(message = "Trạng thái không được để trống")
        @Pattern(regexp = "HOAT_DONG|BI_KHOA|NGUNG_HOAT_DONG",
                 message = "Trạng thái không hợp lệ")
        private String trangThai;
    }

    /** Admin điều chỉnh điểm tích lũy thủ công */
    @Data
    public static class AdjustPointRequest {
        @NotNull(message = "Số điểm không được để trống")
        private Integer diemThayDoi; // dương = cộng, âm = trừ

        @NotBlank(message = "Lý do không được để trống")
        @Size(max = 300)
        private String moTa;
    }
}