package com.poly.cake.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.poly.cake.dto.SanPhamDto;
import com.poly.cake.service.AdminSanPhamService;

import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSanPhamController {

    @Autowired
    private AdminSanPhamService adminSanPhamService;

    @GetMapping
    public ResponseEntity<?> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) Long danhMucId) {

        return ResponseEntity.ok(
                adminSanPhamService.getFilteredProducts(
                        keyword,
                        trangThai,
                        danhMucId
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                adminSanPhamService.getProductById(id)
        );
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody SanPhamDto.Request request) {

        return ResponseEntity.ok(
                adminSanPhamService.createProduct(request)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody SanPhamDto.Request request) {

        return ResponseEntity.ok(
                adminSanPhamService.updateProduct(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {

        adminSanPhamService.deleteProduct(id);

        return ResponseEntity.ok("Xóa sản phẩm thành công");
    }
}