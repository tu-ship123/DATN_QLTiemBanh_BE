package com.poly.cake.controller;

import com.poly.cake.dto.DiemVoucherDto;
import com.poly.cake.service.DiemThuongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint hệ thống điểm thưởng & voucher cá nhân.
 *
 * Base path: /api/v1/loyalty
 *
 * Các endpoint /diem/** và /voucher/** chỉ KHACH_HANG được truy cập.
 * Endpoint /pos/cong-diem dành cho NHAN_VIEN hoặc ADMIN.
 */
@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
public class DiemVoucherController {

    private final DiemThuongService diemThuongService;

    // ─── ĐIỂM ────────────────────────────────────────────────────────────────

    /** GET /api/v1/loyalty/diem – Xem tổng điểm + lịch sử */
    @GetMapping("/diem")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<DiemVoucherDto.DiemTongQuan> layDiem(Authentication auth) {
        return ResponseEntity.ok(diemThuongService.layDiemTongQuan(auth.getName()));
    }

    // ─── VOUCHER ─────────────────────────────────────────────────────────────

    /** GET /api/v1/loyalty/voucher – Danh sách voucher của khách */
    @GetMapping("/voucher")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<List<DiemVoucherDto.VoucherResponse>> layVoucher(Authentication auth) {
        return ResponseEntity.ok(diemThuongService.layVoucherCuaKhach(auth.getName()));
    }

    /** GET /api/v1/loyalty/voucher/goi – Các mã giảm giá có thể đổi bằng điểm */
    @GetMapping("/voucher/goi")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<List<DiemVoucherDto.MaGiamGiaResponse>> layDanhSachGoi(Authentication auth) {
        return ResponseEntity.ok(diemThuongService.layDanhSachMaDoiDiem(auth.getName()));
    }

    /** POST /api/v1/loyalty/voucher/doi-diem – Đổi điểm lấy voucher */
    @PostMapping("/voucher/doi-diem")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<?> doiDiem(@Valid @RequestBody DiemVoucherDto.DoiDiemRequest request, Authentication auth) {
        DiemVoucherDto.VoucherResponse result = diemThuongService.doiDiem(auth.getName(), request);
        return ResponseEntity.ok(result);
    }

    // ─── XÁC NHẬN NHẬN HÀNG (ONLINE) → cộng điểm ───────────────────────────

    /**
     * PUT /api/v1/loyalty/xac-nhan-nhan-hang/{donHangId}
     * Khách bấm "Đã nhận hàng" → đơn chuyển HOAN_THANH + cộng điểm.
     */
    @PutMapping("/xac-nhan-nhan-hang/{donHangId}")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<?> xacNhanNhanHang(@PathVariable Long donHangId, Authentication auth) {
        diemThuongService.congDiemXacNhanNhanHang(donHangId, auth.getName());
        return ResponseEntity.ok("Xác nhận thành công! Điểm thưởng đã được cộng vào tài khoản.");
    }

    // ─── ĐÁNH GIÁ → cộng điểm (gọi nội bộ sau khi lưu DanhGia) ─────────────

    /**
     * POST /api/v1/loyalty/cong-diem-danh-gia/{donHangId}
     * FE gọi ngay sau khi submit đánh giá thành công.
     */
    @PostMapping("/cong-diem-danh-gia/{donHangId}")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<?> congDiemDanhGia(@PathVariable Long donHangId, Authentication auth) {
        diemThuongService.congDiemDanhGia(donHangId, auth.getName());
        return ResponseEntity.ok("Cảm ơn bạn đã đánh giá! +5 điểm đã được cộng.");
    }

    // ─── POS: cộng điểm offline ──────────────────────────────────────────────

    /**
     * POST /api/v1/loyalty/pos/cong-diem
     * Nhân viên nhập SĐT khách → hệ thống tìm & cộng điểm.
     * Nếu không tìm thấy thì trả về timThayKhach=false, không báo lỗi.
     */
    @PostMapping("/pos/cong-diem")
    @PreAuthorize("hasAnyRole('NHAN_VIEN', 'ADMIN')")
    public ResponseEntity<DiemVoucherDto.CongDiemPosResponse> congDiemPos(
            @Valid @RequestBody DiemVoucherDto.CongDiemPosRequest request) {
        return ResponseEntity.ok(diemThuongService.congDiemPos(request));
    }
}