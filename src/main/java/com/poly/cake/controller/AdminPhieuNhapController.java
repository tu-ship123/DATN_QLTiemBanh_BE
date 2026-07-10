package com.poly.cake.controller;


import com.poly.cake.dto.PhieuNhapDto;
import com.poly.cake.entity.PhieuNhapKho;
import com.poly.cake.service.PhieuNhapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/phieu-nhap")
@RequiredArgsConstructor
public class AdminPhieuNhapController {

    private final PhieuNhapService phieuNhapService;

    // API tạo phiếu nhập kho (Ví dụ tạm lấy ID người tạo = 1, thực tế bạn lấy từ JWT / SecurityContext)
    @PostMapping("/create")
    public ResponseEntity<PhieuNhapKho> createPhieuNhap(@RequestBody PhieuNhapDto request) {
        Long nguoiTaoId = 1L; 
        PhieuNhapKho phieuMoi = phieuNhapService.taoPhieuNhap(request, nguoiTaoId);
        return ResponseEntity.ok(phieuMoi);
    }

    // API duyệt phiếu nhập kho và tự động cộng dồn tồn kho
    @PutMapping("/{id}/approve")
    public ResponseEntity<String> approvePhieuNhap(@PathVariable Long id) {
        Long adminId = 1L; // Thực tế lấy từ thông tin Admin đăng nhập
        phieuNhapService.duyetPhieuNhap(id, adminId);
        return ResponseEntity.ok("Duyệt phiếu nhập kho thành công! Kho hàng đã được cập nhật.");
    }
}