package com.poly.cake.controller;

import com.poly.cake.dto.DanhMucDto;
import com.poly.cake.service.AdminDanhMucService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor

public class AdminDanhMucController {

    private final AdminDanhMucService adminDanhMucService;

    // GET ALL
    @GetMapping
    public ResponseEntity<List<DanhMucDto.Response>> getAll() {
        return ResponseEntity.ok(
                adminDanhMucService.getAllDanhMuc()
        );
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<DanhMucDto.Response> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                adminDanhMucService.getDanhMucById(id)
        );
    }

    // CREATE
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody DanhMucDto.Request request) {
        return ResponseEntity.ok(
                adminDanhMucService.createDanhMuc(request)
        );
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody DanhMucDto.Request request) {

        return ResponseEntity.ok(
                adminDanhMucService.updateDanhMuc(id, request)
        );
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id) {

        adminDanhMucService.deleteDanhMuc(id);

        return ResponseEntity.ok(
                "Xóa danh mục thành công!"
        );
    }
}