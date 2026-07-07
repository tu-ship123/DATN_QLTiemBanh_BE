package com.poly.cake.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * T066: DTO cho API "Hồ sơ cá nhân" — dùng cho chính người dùng đang đăng
 * nhập tự xem/sửa thông tin của mình (khác với KhachHangDto dành cho ADMIN
 * quản lý thông tin của khách khác).
 */
public class HoSoDto {

    /** Thông tin hồ sơ trả về cho chính chủ tài khoản */
    @Data
    public static class ProfileResponse {
        private Long id;
        private String hoTen;
        private String email;
        private String soDienThoai;
        private String anhDaiDien;
        private String quyen;
        private String trangThai;
        private LocalDateTime ngayTao;
    }

    /** Cập nhật thông tin cá nhân / avatar */
    @Data
    public static class UpdateProfileRequest {
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
        private String hoTen;

        // Cho phép để trống (bỏ SĐT), nhưng nếu nhập thì phải đúng định dạng
        @Pattern(regexp = "^$|^(0|\\+84)[0-9]{8,10}$", message = "Số điện thoại không hợp lệ")
        private String soDienThoai;

        // URL ảnh đại diện (đã upload sẵn lên storage/CDN ở phía FE), backend
        // chỉ lưu đường dẫn, không xử lý upload file trực tiếp
        @Size(max = 500, message = "Đường dẫn ảnh đại diện quá dài")
        private String anhDaiDien;
    }

    /** Đổi mật khẩu */
    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        private String matKhauHienTai;

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
        private String matKhauMoi;
    }
}