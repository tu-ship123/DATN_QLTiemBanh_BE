package com.poly.cake.controller;

import com.poly.cake.dto.MaGiamGiaDto;
import com.poly.cake.service.AdminMaGiamGiaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMaGiamGiaController {

    @Autowired
    private AdminMaGiamGiaService adminMaGiamGiaService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(
                adminMaGiamGiaService.getAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                adminMaGiamGiaService.getById(id)
        );
    }

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody MaGiamGiaDto.Request request) {

        return ResponseEntity.ok(
                adminMaGiamGiaService.create(request)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody MaGiamGiaDto.Request request) {

        return ResponseEntity.ok(
                adminMaGiamGiaService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id) {

        adminMaGiamGiaService.delete(id);

        return ResponseEntity.ok("Xóa mã giảm giá thành công");
    }
}