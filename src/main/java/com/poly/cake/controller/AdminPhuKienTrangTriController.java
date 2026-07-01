package com.poly.cake.controller;

import com.poly.cake.service.AdminPhuKienTrangTriService;
import com.poly.cake.service.AdminPhuKienTrangTriService.PhuKienRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/decor-items")
public class AdminPhuKienTrangTriController {

    @Autowired
    private AdminPhuKienTrangTriService service;

    /** GET /api/v1/admin/decor-items?keyword=&hoatDong= */
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean hoatDong) {
        return ResponseEntity.ok(service.getAll(keyword, hoatDong));
    }

    /** GET /api/v1/admin/decor-items/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    /** POST /api/v1/admin/decor-items */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody PhuKienRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    /** PUT /api/v1/admin/decor-items/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody PhuKienRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    /** DELETE /api/v1/admin/decor-items/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok("Xóa phụ kiện thành công");
    }

    /** PATCH /api/v1/admin/decor-items/{id}/toggle */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(service.toggle(id));
    }
}
