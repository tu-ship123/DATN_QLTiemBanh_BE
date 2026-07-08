package com.poly.cake.controller;

import com.poly.cake.dto.AuthDto.*;
import com.poly.cake.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Đăng ký thành công!");
    }

    // T008
    @PostMapping("/login")

    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
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
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok("Nếu email đã đăng ký, mã OTP sẽ được gửi đến hộp thư của bạn.");
    }

    // T010: Khôi phục mật khẩu
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Đặt lại mật khẩu thành công!");
    }

    // T065: Đăng ký OTP SĐT - Bước 1: Gửi mã OTP về số điện thoại
    @PostMapping("/otp/send")
    public ResponseEntity<String> sendRegisterOtp(@Valid @RequestBody SendPhoneOtpRequest request) {
        authService.sendRegisterOtp(request);
        return ResponseEntity.ok("Mã OTP đã được gửi đến số điện thoại của bạn.");
    }

    // T065: Đăng ký OTP SĐT - Bước 2: Xác thực OTP -> tạo tài khoản -> tự động đăng nhập
    @PostMapping("/otp/verify")
    public ResponseEntity<AuthResponse> verifyRegisterOtp(@Valid @RequestBody VerifyPhoneOtpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.verifyRegisterOtp(request));
    }

    // T065: Đăng nhập Google OAuth2 - lần đầu tự tạo tài khoản, các lần sau tự đăng nhập
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }

    // T091: 2FA TOTP Setup
    @GetMapping("/totp/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TotpSetupResponse> setupTotp(Authentication authentication) {
        return ResponseEntity.ok(authService.setupTotp(authentication.getName()));
    }

    // T091: 2FA TOTP Verify
    @PostMapping("/totp/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> verifyTotp(@RequestParam String code, Authentication authentication) {
        authService.verifyAndEnableTotp(authentication.getName(), code);
        return ResponseEntity.ok("Xác thực 2 bước đã được kích hoạt thành công!");
    }

    // T091: 2FA TOTP Disable
    @PostMapping("/totp/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> disableTotp(Authentication authentication) {
        authService.disableTotp(authentication.getName());
        return ResponseEntity.ok("Đã tắt xác thực 2 bước!");
    }
}