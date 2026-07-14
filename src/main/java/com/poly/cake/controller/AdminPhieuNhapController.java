package com.poly.cake.controller;


import com.poly.cake.dto.PhieuNhapDto;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.PhieuNhapKho;
import com.poly.cake.exception.ResourceNotFoundException;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.service.PhieuNhapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/phieu-nhap")
@RequiredArgsConstructor
// LƯU Ý BẢO MẬT: path này là "/api/admin/..." (thiếu "v1") nên KHÔNG khớp rule
// "/api/v1/admin/**" trong SecurityConfig -> nếu không có @PreAuthorize ở đây thì
// bất kỳ user đã đăng nhập nào (kể cả khách hàng) cũng gọi được API này.
@PreAuthorize("hasRole('ADMIN')")
public class AdminPhieuNhapController {

    private final PhieuNhapService phieuNhapService;
    private final NguoiDungRepository nguoiDungRepository;

    private Long currentUserId(Authentication authentication) {
        NguoiDung user = nguoiDungRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại!"));
        return user.getId();
    }

    // API lấy danh sách phiếu nhập kho, mới nhất lên đầu
    @GetMapping
    public ResponseEntity<?> getDanhSach() {
        return ResponseEntity.ok(phieuNhapService.layDanhSach());
    }

    // API tạo phiếu nhập kho - LƯU Ý: trước đây hard-code nguoiTaoId = 1L, đã sửa
    // để lấy đúng ID của Admin đang đăng nhập (tránh ghi sai người tạo phiếu).
    @PostMapping("/create")
    public ResponseEntity<PhieuNhapKho> createPhieuNhap(Authentication authentication, @RequestBody PhieuNhapDto request) {
        PhieuNhapKho phieuMoi = phieuNhapService.taoPhieuNhap(request, currentUserId(authentication));
        return ResponseEntity.ok(phieuMoi);
    }

    // API duyệt phiếu nhập kho và tự động cộng dồn tồn kho - cùng lỗi, đã sửa tương tự
    @PutMapping("/{id}/approve")
    public ResponseEntity<String> approvePhieuNhap(Authentication authentication, @PathVariable Long id) {
        phieuNhapService.duyetPhieuNhap(id, currentUserId(authentication));
        return ResponseEntity.ok("Duyệt phiếu nhập kho thành công! Kho hàng đã được cập nhật.");
    }
}
