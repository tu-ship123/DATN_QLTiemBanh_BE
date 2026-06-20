package com.poly.cake.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

public class StaffDto {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Họ tên không được để trống")
        private String hoTen;

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        private String email;

        @Pattern(regexp = "^(0|\\+84)[0-9]{8,10}$", message = "Số điện thoại không hợp lệ")
        private String soDienThoai;

        // TUYỆT ĐỐI KHÔNG CÓ id, quyen, matKhau ở đây
    }

    @Data
    public static class UpdateRequest {
        @NotBlank(message = "Họ tên không được để trống")
        private String hoTen;

        @Pattern(regexp = "^(0|\\+84)[0-9]{8,10}$", message = "Số điện thoại không hợp lệ")
        private String soDienThoai;

        private String trangThai; // Quản lý có thể khóa tài khoản nhân viên
    }
}