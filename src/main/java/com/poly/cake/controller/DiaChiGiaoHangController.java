package com.poly.cake.controller;

import com.poly.cake.dto.DiaChiDto;
import com.poly.cake.entity.DiaChiGiaoHang;
import com.poly.cake.service.DiaChiGiaoHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Sổ địa chỉ giao hàng của KHÁCH HÀNG đang đăng nhập.
 * FIX: trước đây dùng MOCK_USER_ID = 1L (giả lập) khiến MỌI khách hàng đều
 * đọc/ghi chung 1 sổ địa chỉ của user #1 — nay lấy đúng người dùng thật từ
 * Authentication (giống pattern của GioHangController/HoSoController).
 */
@RestController
@RequestMapping("/api/v1/dia-chi")
@CrossOrigin("*")
@PreAuthorize("isAuthenticated()")
public class DiaChiGiaoHangController {

    @Autowired
    private DiaChiGiaoHangService diaChiService;

    @GetMapping
    public ResponseEntity<List<DiaChiGiaoHang>> getAll(Authentication authentication) {
        return ResponseEntity.ok(diaChiService.getDanhSachDiaChi(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<DiaChiGiaoHang> create(Authentication authentication, @RequestBody DiaChiDto request) {
        return ResponseEntity.ok(diaChiService.themDiaChi(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiaChiGiaoHang> update(Authentication authentication, @PathVariable Long id, @RequestBody DiaChiDto request) {
        return ResponseEntity.ok(diaChiService.capNhatDiaChi(authentication.getName(), id, request));
    }

    // Đặt 1 địa chỉ đã lưu sẵn làm địa chỉ mặc định (không cần gửi lại toàn bộ form)
    @PutMapping("/{id}/mac-dinh")
    public ResponseEntity<DiaChiGiaoHang> datMacDinh(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(diaChiService.datMacDinh(authentication.getName(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(Authentication authentication, @PathVariable Long id) {
        diaChiService.xoaDiaChi(authentication.getName(), id);
        return ResponseEntity.ok("Xóa địa chỉ thành công!");
    }
}
