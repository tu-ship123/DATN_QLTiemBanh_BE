package com.poly.cake.controller;

import com.poly.cake.dto.GioHangDto;
import com.poly.cake.service.GioHangService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
public class GioHangController {

    @Autowired
    private GioHangService gioHangService;

    @GetMapping
    public ResponseEntity<?> layGioHang(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Chưa đăng nhập");
        }
        String email = authentication.getName();
        return ResponseEntity.ok(gioHangService.layGioHang(email));
    }

    // POST /api/v1/cart/items — Thêm sản phẩm vào giỏ
    @PostMapping("/items")
    public ResponseEntity<?> themVaoGio(
            @Valid @RequestBody GioHangDto.ThemVaoGioRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(gioHangService.themVaoGio(email, request));
    }

    // PUT /api/v1/cart/items/{chiTietId} — Cập nhật số lượng
    @PutMapping("/items/{chiTietId}")
    public ResponseEntity<?> capNhatSoLuong(
            @PathVariable Long chiTietId,
            @Valid @RequestBody GioHangDto.CapNhatSoLuongRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(gioHangService.capNhatSoLuong(email, chiTietId, request));
    }

    // DELETE /api/v1/cart/items/{chiTietId} — Xóa 1 sản phẩm
    @DeleteMapping("/items/{chiTietId}")
    public ResponseEntity<?> xoaKhoiGio(
            @PathVariable Long chiTietId,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(gioHangService.xoaKhoiGio(email, chiTietId));
    }

    // DELETE /api/v1/cart — Xóa toàn bộ giỏ hàng
    @DeleteMapping
    public ResponseEntity<?> xoaToanBoGio(Authentication authentication) {
        String email = authentication.getName();
        gioHangService.xoaToanBoGio(email);
        return ResponseEntity.ok("Đã xóa toàn bộ giỏ hàng!");
    }
}
