package com.poly.cake.controller;

import com.poly.cake.dto.PhanCaRequest;
import com.poly.cake.service.PhanCaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class PhanCaController {

    @Autowired
    private PhanCaService phanCaService;

    // Phân ca cho nhân viên
    @PostMapping("/schedules")
    public ResponseEntity<?> createSchedule(@Valid @RequestBody PhanCaRequest request) {
        try {
            return ResponseEntity.ok(phanCaService.createPhanCa(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Xem bảng chấm công
    @GetMapping("/attendances")
    public ResponseEntity<?> getAttendances() {
        return ResponseEntity.ok(phanCaService.getAllChamCong());
    }
}