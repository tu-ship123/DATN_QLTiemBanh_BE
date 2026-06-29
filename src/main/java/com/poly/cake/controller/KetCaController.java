package com.poly.cake.controller;

import com.poly.cake.dto.KetCaRequest;
import com.poly.cake.service.KetCaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * T062 – Kết Ca: X-Report / Z-Report
 *
 * Dữ liệu kết ca được lưu thẳng vào bảng cham_cong hiện có (không tạo bảng mới).
 *
 * ── Nhân viên ────────────────────────────────────────────────────────────────
 *   POST /api/v1/staff/ket-ca
 *       Body: { phanCaId, loaiBaoCao ("X_REPORT"|"Z_REPORT"), ghiChu? }
 *
 *   GET  /api/v1/staff/ket-ca/{phanCaId}
 *       → Xem báo cáo kết ca của ca đó (chỉ ca của mình).
 *
 * ── Admin ─────────────────────────────────────────────────────────────────────
 *   GET  /api/v1/admin/ket-ca?ngay=2025-06-28
 *       → Tất cả báo cáo kết ca (X + Z) trong ngày chỉ định.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "T062 – Kết Ca", description = "X-Report / Z-Report: kết ca + tổng hợp doanh thu tiền mặt/SePay")
public class KetCaController {

    private final KetCaService ketCaService;

    // ── NHÂN VIÊN ─────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/staff/ket-ca")
    @PreAuthorize("hasAnyRole('NHAN_VIEN', 'ADMIN')")
    @Operation(
        summary = "Kết ca – sinh X-Report hoặc Z-Report",
        description = "Nhân viên đã check-in nhấn kết ca:\n"
            + "- X_REPORT: tổng hợp doanh thu xem tạm, ca vẫn mở, ghi đè được nhiều lần.\n"
            + "- Z_REPORT: kết ca chính thức, ghi doanh thu vào ChamCong,\n"
            + "  PhanCa → DA_KET_CA, ghi giờ ra nếu chưa checkout.\n"
            + "  Chỉ thực hiện được 1 lần."
    )
    public ResponseEntity<?> ketCa(@RequestBody KetCaRequest request) {
        try {
            return ResponseEntity.ok(ketCaService.ketCa(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/v1/staff/ket-ca/{phanCaId}")
    @PreAuthorize("hasAnyRole('NHAN_VIEN', 'ADMIN')")
    @Operation(
        summary = "Xem báo cáo kết ca của một ca",
        description = "Trả về ChamCong kèm dữ liệu X/Z Report của ca đó (chỉ ca của mình)."
    )
    public ResponseEntity<?> getBaoCaoByPhanCa(@PathVariable Long phanCaId) {
        try {
            return ResponseEntity.ok(ketCaService.getBaoCaoByPhanCa(phanCaId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/admin/ket-ca")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "[Admin] Tất cả báo cáo kết ca trong ngày",
        description = "Không truyền ngay → hôm nay. Trả về các ChamCong đã có X hoặc Z Report."
    )
    public ResponseEntity<?> getAdminBaoCaoTheoNgay(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
        return ResponseEntity.ok(ketCaService.getAdminBaoCaoTheoNgay(ngay));
    }
}