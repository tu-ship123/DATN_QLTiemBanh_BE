package com.poly.cake.controller;

import com.poly.cake.dto.DanhGiaDto;
import com.poly.cake.service.DanhGiaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints đánh giá sản phẩm.
 *
 * POST /api/v1/orders/{donHangId}/review          – Gửi đánh giá (KHACH_HANG)
 * GET  /api/v1/orders/{donHangId}/review          – Xem đánh giá của đơn (KHACH_HANG)
 * GET  /api/v1/products/{sanPhamId}/reviews       – Đánh giá công khai của sản phẩm (ALL)
 */
@RestController
@RequiredArgsConstructor
public class DanhGiaController {

    private final DanhGiaService danhGiaService;

    // ─── Khách hàng gửi đánh giá ─────────────────────────────────────────────

    @PostMapping("/api/v1/orders/{donHangId}/review")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<?> guiDanhGia(
            @PathVariable Long donHangId,
            @Valid @RequestBody DanhGiaDto.Request request,
            Authentication auth) {
        DanhGiaDto.Response result = danhGiaService.guiDanhGia(donHangId, request, auth.getName());
        return ResponseEntity.ok(result);
    }

    // ─── Lấy trạng thái đánh giá của đơn hàng ───────────────────────────────

    @GetMapping("/api/v1/orders/{donHangId}/review")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<?> layDanhGiaTheoDon(
            @PathVariable Long donHangId,
            Authentication auth) {
        DanhGiaDto.DonHangDanhGiaResponse result =
                danhGiaService.layDanhGiaTheoDon(donHangId, auth.getName());
        return ResponseEntity.ok(result);
    }

    // ─── Đánh giá công khai của sản phẩm (không cần đăng nhập) ──────────────

    @GetMapping("/api/v1/products/{sanPhamId}/reviews")
    public ResponseEntity<List<DanhGiaDto.Response>> layDanhGiaTheoSanPham(
            @PathVariable Long sanPhamId) {
        return ResponseEntity.ok(danhGiaService.layDanhGiaTheoSanPham(sanPhamId));
    }
}