package com.poly.cake.dto;

import lombok.Data;

public class AuthDto {
    
    @Data
    public static class RegisterRequest {
        private String hoTen;
        private String email;
        private String matKhau;
        private String soDienThoai;
    }

    @Data
    public static class LoginRequest {
        private String email;
        private String matKhau;
    }

    @Data
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
    }

    @Data
    public static class ForgotPasswordRequest {
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        private String email;
        private String otp;
        private String newPassword;
    }
}