package com.poly.cake.controller;


import com.poly.cake.dto.PhieuNhapDto;
import com.poly.cake.dto.PhieuNhapResponseDto;
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
    //
    // FIX: trước đây trả thẳng entity PhieuNhapKho ra ngoài -> bị lỗi vòng lặp
    // tham chiếu 2 chiều khi Jackson serialize (PhieuNhapKho.chiTietList chứa
    // ChiTietPhieuNhap, mà ChiTietPhieuNhap lại trỏ ngược lại PhieuNhapKho) ->
    // JSON trả về bị hỏng/thiếu field "id" -> FE gọi duyệt phiếu bị lỗi
    // "undefined" (PUT /phieu-nhap/undefined/approve). Giờ convert sang DTO
    // trước khi trả về, đảm bảo luôn có "id" hợp lệ và không bị lặp vô hạn.
    @PostMapping("/create")
    public ResponseEntity<PhieuNhapResponseDto> createPhieuNhap(Authentication authentication, @RequestBody PhieuNhapDto request) {
        PhieuNhapKho phieuMoi = phieuNhapService.taoPhieuNhap(request, currentUserId(authentication));
        return ResponseEntity.ok(phieuNhapService.toResponseDto(phieuMoi));
    }

    // API duyệt phiếu nhập kho và tự động cộng dồn tồn kho - cùng lỗi, đã sửa tương tự
    @PutMapping("/{id}/approve")
    public ResponseEntity<String> approvePhieuNhap(Authentication authentication, @PathVariable Long id) {
        phieuNhapService.duyetPhieuNhap(id, currentUserId(authentication));
        return ResponseEntity.ok("Duyệt phiếu nhập kho thành công! Kho hàng đã được cập nhật.");
    }
}