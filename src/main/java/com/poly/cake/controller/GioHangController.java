package com.poly.cake.controller;

import com.poly.cake.dto.GioHangDto;
import com.poly.cake.service.GioHangService;
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
            try {
                if (authentication == null || !authentication.isAuthenticated()) {
                    return ResponseEntity.status(401).body("Chưa đăng nhập");
                }
                String email = authentication.getName();
                return ResponseEntity.ok(gioHangService.layGioHang(email));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }

    // POST /api/v1/cart/items — Thêm sản phẩm vào giỏ
    @PostMapping("/items")
    public ResponseEntity<?> themVaoGio(
            @RequestBody GioHangDto.ThemVaoGioRequest request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            return ResponseEntity.ok(gioHangService.themVaoGio(email, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /api/v1/cart/items/{chiTietId} — Cập nhật số lượng
    @PutMapping("/items/{chiTietId}")
    public ResponseEntity<?> capNhatSoLuong(
            @PathVariable Long chiTietId,
            @RequestBody GioHangDto.CapNhatSoLuongRequest request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            return ResponseEntity.ok(gioHangService.capNhatSoLuong(email, chiTietId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /api/v1/cart/items/{chiTietId} — Xóa 1 sản phẩm
    @DeleteMapping("/items/{chiTietId}")
    public ResponseEntity<?> xoaKhoiGio(
            @PathVariable Long chiTietId,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            return ResponseEntity.ok(gioHangService.xoaKhoiGio(email, chiTietId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /api/v1/cart — Xóa toàn bộ giỏ hàng
    @DeleteMapping
    public ResponseEntity<?> xoaToanBoGio(Authentication authentication) {
        try {
            String email = authentication.getName();
            gioHangService.xoaToanBoGio(email);
            return ResponseEntity.ok("Đã xóa toàn bộ giỏ hàng!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
