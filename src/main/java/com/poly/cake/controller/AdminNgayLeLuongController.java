package com.poly.cake.controller;

import com.poly.cake.dto.NgayLeLuongDto;
import com.poly.cake.service.NgayLeLuongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * T102 – Admin cấu hình ngày lễ để tự động nhân hệ số lương (x2, x3...)
 * khi nhân viên chấm công vào đúng ngày đó.
 */
@RestController
@RequestMapping("/api/v1/admin/ngay-le-luong")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminNgayLeLuongController {

    private final NgayLeLuongService ngayLeLuongService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(ngayLeLuongService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ngayLeLuongService.getById(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody NgayLeLuongDto.Request request) {
        return ResponseEntity.ok(ngayLeLuongService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody NgayLeLuongDto.Request request) {
        return ResponseEntity.ok(ngayLeLuongService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        ngayLeLuongService.delete(id);
        return ResponseEntity.ok("Xóa cấu hình ngày lễ thành công");
    }
}