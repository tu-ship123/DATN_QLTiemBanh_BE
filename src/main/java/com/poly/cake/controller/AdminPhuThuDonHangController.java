package com.poly.cake.controller;

import com.poly.cake.dto.PhuThuDonHangDto;
import com.poly.cake.service.PhuThuDonHangService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * T102 – Admin cấu hình phụ thu cho các dịp đặc biệt (Tết, Valentine...).
 * Đơn hàng có ngày giao rơi vào dịp này sẽ tự động cộng thêm % vào tiền hàng.
 */
@RestController
@RequestMapping("/api/v1/admin/phu-thu")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPhuThuDonHangController {

    private final PhuThuDonHangService phuThuDonHangService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(phuThuDonHangService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(phuThuDonHangService.getById(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody PhuThuDonHangDto.Request request) {
        return ResponseEntity.ok(phuThuDonHangService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody PhuThuDonHangDto.Request request) {
        return ResponseEntity.ok(phuThuDonHangService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        phuThuDonHangService.delete(id);
        return ResponseEntity.ok("Xóa cấu hình phụ thu thành công");
    }
}