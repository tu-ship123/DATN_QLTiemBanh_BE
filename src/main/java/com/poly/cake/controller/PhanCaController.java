package com.poly.cake.controller;

import com.poly.cake.dto.CaLamViecRequest;
import com.poly.cake.dto.PhanCaRequest;
import com.poly.cake.service.PhanCaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller quản lý Ca Làm Việc và Phân Ca — chỉ ADMIN được dùng.
 *
 * Endpoints:
 *   GET    /api/v1/admin/ca-lam-viec             → Danh sách ca (FE dùng cho dropdown)
 *   POST   /api/v1/admin/ca-lam-viec             → Tạo ca mới
 *   PUT    /api/v1/admin/ca-lam-viec/{id}        → Cập nhật ca
 *   DELETE /api/v1/admin/ca-lam-viec/{id}        → Vô hiệu hoá ca
 *
 *   POST   /api/v1/admin/schedules               → Phân ca cho nhân viên
 *   GET    /api/v1/admin/schedules?date=...      → Xem lịch phân ca theo ngày
 *   DELETE /api/v1/admin/schedules/{id}          → Huỷ phân ca
 *
 *   GET    /api/v1/admin/attendances             → Bảng chấm công tổng hợp
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class PhanCaController {

    private final PhanCaService phanCaService;

    // ── Ca Làm Việc ──────────────────────────────────────────────────────────

    @GetMapping("/ca-lam-viec")
    public ResponseEntity<?> getAllCaLamViec() {
        return ResponseEntity.ok(phanCaService.getAllCaLamViec());
    }

    @PostMapping("/ca-lam-viec")
    public ResponseEntity<?> createCaLamViec(@Valid @RequestBody CaLamViecRequest request) {
        try {
            return ResponseEntity.ok(phanCaService.createCaLamViec(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/ca-lam-viec/{id}")
    public ResponseEntity<?> updateCaLamViec(
            @PathVariable Long id,
            @Valid @RequestBody CaLamViecRequest request) {
        try {
            return ResponseEntity.ok(phanCaService.updateCaLamViec(id, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/ca-lam-viec/{id}")
    public ResponseEntity<?> deleteCaLamViec(@PathVariable Long id) {
        try {
            phanCaService.deleteCaLamViec(id);
            return ResponseEntity.ok("Đã vô hiệu hoá ca làm việc");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Phân Ca ──────────────────────────────────────────────────────────────

    @PostMapping("/schedules")
    public ResponseEntity<?> createSchedule(@Valid @RequestBody PhanCaRequest request) {
        try {
            return ResponseEntity.ok(phanCaService.createPhanCa(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Xem lịch phân ca theo ngày.
     * Ví dụ: GET /api/v1/admin/schedules?date=2025-06-25
     * Không truyền date → trả về hôm nay.
     */
    @GetMapping("/schedules")
    public ResponseEntity<?> getSchedulesByDate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(phanCaService.getPhanCaTheoNgay(date));
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<?> cancelSchedule(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(phanCaService.huyPhanCa(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Chấm Công ────────────────────────────────────────────────────────────

    @GetMapping("/attendances")
    public ResponseEntity<?> getAttendances() {
        return ResponseEntity.ok(phanCaService.getAllChamCong());
    }
}
