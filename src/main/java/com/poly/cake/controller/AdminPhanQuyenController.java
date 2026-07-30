package com.poly.cake.controller;

import com.poly.cake.entity.NguoiDung;
import com.poly.cake.exception.NgoaiLeNghiepVu;
import com.poly.cake.exception.NgoaiLeKhongTimThayTaiNguyen;
import com.poly.cake.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Trang RBAC (Admin): hệ thống hiện tại CHỈ có 3 quyền phẳng (ADMIN, NHAN_VIEN,
 * KHACH_HANG) — chưa có ma trận phân quyền chi tiết theo từng chức năng.
 * API này cho phép xem tổng quan số lượng tài khoản theo từng quyền và đổi
 * quyền của 1 tài khoản (VD: nâng khách hàng lên nhân viên).
 * Path "/api/v1/admin/**" đã được bảo vệ ROLE_ADMIN sẵn trong CauHinhBaoMat.
 */
@RestController
@RequestMapping("/api/v1/admin/rbac")
@RequiredArgsConstructor
public class AdminPhanQuyenController {

    private final NguoiDungRepository nguoiDungRepository;

    private static final List<Map<String, Object>> MO_TA_QUYEN = List.of(
            Map.of("quyen", "ADMIN", "moTa", "Toàn quyền quản trị hệ thống: sản phẩm, đơn hàng, nhân sự, báo cáo, cấu hình."),
            Map.of("quyen", "NHAN_VIEN", "moTa", "Xử lý đơn hàng, chấm công, xem ca làm việc của bản thân."),
            Map.of("quyen", "KHACH_HANG", "moTa", "Đặt hàng, quản lý địa chỉ/thiết kế yêu thích, xem lịch sử đơn hàng của bản thân.")
    );

    @GetMapping("/roles")
    public ResponseEntity<?> getRoles() {
        List<Map<String, Object>> roles = MO_TA_QUYEN.stream().map(r -> {
            String quyen = (String) r.get("quyen");
            return Map.of(
                    "quyen", quyen,
                    "moTa", r.get("moTa"),
                    "soLuongTaiKhoan", nguoiDungRepository.countByQuyen(quyen)
            );
        }).toList();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsersByRole(@RequestParam(required = false) String quyen) {
        if (quyen == null || quyen.isBlank()) {
            return ResponseEntity.ok(nguoiDungRepository.findAll());
        }
        return ResponseEntity.ok(nguoiDungRepository.findByQuyen(quyen));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> changeRole(Authentication authentication, @PathVariable Long id, @RequestBody Map<String, String> body) {
        String quyenMoi = body.get("quyen");
        if (quyenMoi == null || !List.of("ADMIN", "NHAN_VIEN", "KHACH_HANG").contains(quyenMoi)) {
            throw new NgoaiLeNghiepVu("Quyền không hợp lệ! Chỉ chấp nhận ADMIN, NHAN_VIEN hoặc KHACH_HANG.");
        }

        NguoiDung user = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen("Không tìm thấy tài khoản!"));

        // Chặn tự hạ quyền chính mình để tránh tự khóa mất quyền Admin giữa chừng
        if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(authentication.getName()) && !"ADMIN".equals(quyenMoi)) {
            throw new NgoaiLeNghiepVu("Không thể tự hạ quyền của chính tài khoản đang đăng nhập!");
        }

        user.setQuyen(quyenMoi);
        return ResponseEntity.ok(nguoiDungRepository.save(user));
    }
}
