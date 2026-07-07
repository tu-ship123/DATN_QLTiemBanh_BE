package com.poly.cake.controller;

import com.poly.cake.dto.VoucherValidateDto;
import com.poly.cake.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * T070 – Base path: /api/v1/vouchers
 *
 * Endpoint kiểm tra mã giảm giá/voucher TRƯỚC khi đặt hàng. Yêu cầu đăng nhập
 * (khách hàng) — xem SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    /**
     * POST /api/v1/vouchers/validate
     * Kiểm tra full điều kiện (hoạt động, hạn dùng, số lượt, đơn tối thiểu...)
     * của 1 mã giảm giá (maCode) hoặc 1 voucher cá nhân (voucherKhachHangId).
     */
    @PostMapping("/validate")
    public ResponseEntity<VoucherValidateDto.Response> validate(
            @Valid @RequestBody VoucherValidateDto.Request request,
            Authentication authentication) {

        return ResponseEntity.ok(voucherService.validate(request, authentication.getName()));
    }
}
