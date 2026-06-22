package com.poly.cake.controller;

import com.poly.cake.dto.DanhMucDto;
import com.poly.cake.service.AdminDanhMucService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class KhachHangDanhMucController {

    private final AdminDanhMucService adminDanhMucService;

    /**
     * API công khai - lấy tất cả danh mục đang hoạt động
     * GET /api/v1/categories
     */
    @GetMapping
    public ResponseEntity<List<DanhMucDto.Response>> getActiveCategories() {
        List<DanhMucDto.Response> all = adminDanhMucService.getAllDanhMuc();

        // Chỉ trả về danh mục đang hoạt động (hoatDong = true)
        List<DanhMucDto.Response> active = all.stream()
                .filter(dm -> Boolean.TRUE.equals(dm.getHoatDong()))
                .toList();

        return ResponseEntity.ok(active);
    }

    /**
     * API công khai - xem chi tiết 1 danh mục
     * GET /api/v1/categories/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<DanhMucDto.Response> getCategoryById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                adminDanhMucService.getDanhMucById(id)
        );
    }
}