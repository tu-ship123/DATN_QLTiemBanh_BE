package com.poly.cake.controller;

import com.poly.cake.dto.DanhGiaDto;
import com.poly.cake.service.AdminDanhGiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin quản lý đánh giá sản phẩm.
 *
 * GET    /api/v1/admin/reviews              – Lấy tất cả đánh giá (lọc theo sao, sản phẩm, ẩn/hiện)
 * GET    /api/v1/admin/reviews/stats        – Thống kê tổng quan
 * PUT    /api/v1/admin/reviews/{id}/reply   – Phản hồi đánh giá
 * PUT    /api/v1/admin/reviews/{id}/toggle  – Ẩn / Hiện đánh giá
 * DELETE /api/v1/admin/reviews/{id}         – Xóa đánh giá
 */
@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_NHAN_VIEN')")
public class AdminDanhGiaController {

    private final AdminDanhGiaService adminDanhGiaService;

    // GET ALL (có thể lọc)
    @GetMapping
    public ResponseEntity<List<DanhGiaDto.Response>> getAll(
            @RequestParam(required = false) Integer soSao,
            @RequestParam(required = false) Long sanPhamId,
            @RequestParam(required = false) Boolean biAn) {

        return ResponseEntity.ok(
                adminDanhGiaService.getAll(soSao, sanPhamId, biAn)
        );
    }

    // THỐNG KÊ
    @GetMapping("/stats")
    public ResponseEntity<DanhGiaDto.StatsResponse> getStats() {
        return ResponseEntity.ok(adminDanhGiaService.getStats());
    }

    // PHẢN HỒI
    @PutMapping("/{id}/reply")
    public ResponseEntity<?> reply(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(
                    adminDanhGiaService.reply(id, body.get("phanHoi"))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ẨN / HIỆN
    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminDanhGiaService.toggleBiAn(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // XÓA
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            adminDanhGiaService.delete(id);
            return ResponseEntity.ok("Đã xóa đánh giá");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}