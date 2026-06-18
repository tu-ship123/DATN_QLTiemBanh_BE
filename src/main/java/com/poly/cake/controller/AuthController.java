package com.poly.cake.controller;

import com.poly.cake.dto.AuthDto.*;
import com.poly.cake.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // T007
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Đăng ký thành công!");
    }

    // T008
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // T009: Refresh Token
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    // T009: Đăng xuất (yêu cầu Access Token trong header)
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()") // Bổ sung chốt chặn: Phải đăng nhập mới được gọi hàm này
    public ResponseEntity<String> logout(HttpServletRequest request, Authentication authentication) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            String email = authentication.getName();
            authService.logout(accessToken, email);
            return ResponseEntity.ok("Đăng xuất thành công!");
        }
        return ResponseEntity.badRequest().body("Thiếu thông tin token!");
    }
    // T010: Quên mật khẩu
    // [SỬA] Luôn trả về cùng một thông báo dù email có tồn tại hay không
    // → Tránh kẻ tấn công dò được email nào đã đăng ký
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok("Nếu email đã đăng ký, mã OTP sẽ được gửi đến hộp thư của bạn.");
    }

    // T010: Khôi phục mật khẩu
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Đặt lại mật khẩu thành công!");
    }
}