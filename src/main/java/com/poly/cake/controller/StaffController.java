package com.poly.cake.controller;

import com.poly.cake.entity.NguoiDung;
import com.poly.cake.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.poly.cake.dto.StaffDto;
import jakarta.validation.Valid;
import com.poly.cake.dto.StaffDto;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/staff")
// Chỉ Admin mới được quản lý nhân sự
public class StaffController {

    @Autowired
    private StaffService staffService;

    @GetMapping
    public ResponseEntity<?> getAllStaffs() {
        return ResponseEntity.ok(staffService.getAllStaffs());
    }

    // Hiệu suất nhân viên: số đơn xử lý + doanh thu mang lại, lọc được theo khoảng ngày
    @GetMapping("/hieu-suat")
    public ResponseEntity<?> getHieuSuat(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate tuNgay,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate denNgay) {
        java.time.LocalDateTime tu = tuNgay != null ? tuNgay.atStartOfDay() : null;
        java.time.LocalDateTime den = denNgay != null ? denNgay.atTime(23, 59, 59) : null;
        return ResponseEntity.ok(staffService.getHieuSuat(tu, den));
    }

    @PostMapping
    public ResponseEntity<?> createStaff(@Valid @RequestBody StaffDto.CreateRequest request) {
        // Không cần try-catch nữa! GlobalExceptionHandler sẽ tự bắt lỗi nếu có.
        return ResponseEntity.ok(staffService.createStaff(request));
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStaff(@PathVariable Long id, @Valid @RequestBody StaffDto.UpdateRequest request) {
        return ResponseEntity.ok(staffService.updateStaff(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStaff(@PathVariable Long id) {
        staffService.deleteStaff(id);
        return ResponseEntity.ok("Đã khóa tài khoản nhân viên thành công!");
    }
}