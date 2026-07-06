package com.poly.cake.controller;

import com.poly.cake.dto.AuthDto.AuthResponse;
import com.poly.cake.dto.HoSoDto.ChangePasswordRequest;
import com.poly.cake.dto.HoSoDto.ProfileResponse;
import com.poly.cake.dto.HoSoDto.UpdateProfileRequest;
import com.poly.cake.service.HoSoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * T066: API Cập nhật Profile + Đổi mật khẩu.
 * Áp dụng cho MỌI người dùng đã đăng nhập (khách hàng, nhân viên, admin) tự
 * quản lý thông tin của chính mình — không cần khai báo lại trong
 * SecurityConfig vì "/api/v1/users/**" đã rơi vào rule mặc định
 * ".anyRequest().authenticated()".
 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class HoSoController {

    private final HoSoService hoSoService;

    // T066: Xem thông tin hồ sơ cá nhân hiện tại
    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(hoSoService.getProfile(authentication.getName()));
    }

    // T066: Cập nhật thông tin cá nhân / avatar
    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(hoSoService.updateProfile(authentication.getName(), request));
    }

    // T066: Đổi mật khẩu -> tự động đăng xuất tất cả thiết bị khác
    @PutMapping("/password")
    public ResponseEntity<AuthResponse> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(hoSoService.changePassword(authentication.getName(), request));
    }
}