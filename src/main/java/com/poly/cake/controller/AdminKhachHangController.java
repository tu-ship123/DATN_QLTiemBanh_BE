package com.poly.cake.controller;

import com.poly.cake.dto.KhachHangDto;
import com.poly.cake.service.AdminKhachHangService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminKhachHangController {

    @Autowired
    private AdminKhachHangService adminKhachHangService;

    /** Lấy toàn bộ danh sách khách hàng */
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(adminKhachHangService.getAll());
    }

    /** Chi tiết 1 khách hàng */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(adminKhachHangService.getById(id));
    }

    /** Cập nhật họ tên / SĐT / trạng thái */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody KhachHangDto.UpdateRequest request) {
        return ResponseEntity.ok(adminKhachHangService.update(id, request));
    }

    /** Khóa / Mở khóa nhanh */
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(adminKhachHangService.toggleTrangThai(id));
    }

    /** Điều chỉnh điểm tích lũy thủ công */
    @PostMapping("/{id}/adjust-points")
    public ResponseEntity<?> adjustPoints(
            @PathVariable Long id,
            @Valid @RequestBody KhachHangDto.AdjustPointRequest request) {
        return ResponseEntity.ok(adminKhachHangService.adjustDiem(id, request));
    }

    /** Xóa khách hàng */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        adminKhachHangService.delete(id);
        return ResponseEntity.ok("Đã xóa khách hàng thành công");
    }
}