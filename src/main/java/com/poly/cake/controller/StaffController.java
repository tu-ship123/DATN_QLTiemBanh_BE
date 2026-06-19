package com.poly.cake.controller;

import com.poly.cake.entity.NguoiDung;
import com.poly.cake.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/staff")
@PreAuthorize("hasRole('ADMIN')") // Chỉ Admin mới được quản lý nhân sự
public class StaffController {

    @Autowired
    private StaffService staffService;

    @GetMapping
    public ResponseEntity<?> getAllStaffs() {
        return ResponseEntity.ok(staffService.getAllStaffs());
    }

    @PostMapping
    public ResponseEntity<?> createStaff(@RequestBody NguoiDung staff) {
        try {
            return ResponseEntity.ok(staffService.createStaff(staff));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStaff(@PathVariable Long id, @RequestBody NguoiDung staffDetails) {
        try {
            return ResponseEntity.ok(staffService.updateStaff(id, staffDetails));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStaff(@PathVariable Long id) {
        try {
            staffService.deleteStaff(id);
            return ResponseEntity.ok("Đã khóa tài khoản nhân viên thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}