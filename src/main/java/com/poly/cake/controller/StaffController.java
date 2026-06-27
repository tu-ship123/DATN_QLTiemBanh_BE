package com.poly.cake.controller;

import com.poly.cake.entity.NguoiDung;
import com.poly.cake.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.poly.cake.dto.StaffDto;
import jakarta.validation.Valid;

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
    @PreAuthorize("hasRole('ADMIN')")
    // Đổi ResponseEntity<NguoiDung> thành ResponseEntity<StaffDto.Response>
    public ResponseEntity<StaffDto.Response> createStaff(@Valid @RequestBody StaffDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.createStaff(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    // Tương tự cho hàm update
    public ResponseEntity<StaffDto.Response> updateStaff(@PathVariable Long id, @Valid @RequestBody StaffDto.UpdateRequest request) {
        return ResponseEntity.ok(staffService.updateStaff(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStaff(@PathVariable Long id) {

            staffService.deleteStaff(id);
            return ResponseEntity.ok("Đã khóa tài khoản nhân viên thành công!");

    }
}