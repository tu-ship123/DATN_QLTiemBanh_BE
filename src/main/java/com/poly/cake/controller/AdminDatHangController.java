package com.poly.cake.controller;

import com.poly.cake.dto.DatHangDto;
import com.poly.cake.service.AdminDatHangService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN')")
public class AdminDatHangController {

    @Autowired
    private AdminDatHangService adminOrderService;

    // 1. GET: Lọc đơn hàng nâng cao (Dùng param trên URL)
    @GetMapping
    public ResponseEntity<List<DatHangDto.Response>> filterOrders(
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String nguonDon,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime denNgay) {
        return ResponseEntity.ok(adminOrderService.getFilteredOrders(trangThai, nguonDon, tuNgay, denNgay));
    }

    // 2. PUT: Override trạng thái + Audit log
    @PutMapping("/{id}/override")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> overrideOrder(@PathVariable Long id,
                                           @RequestParam String trangThaiMoi,
                                           @RequestParam(required = false) String lyDo,
                                           Authentication authentication) {
        return ResponseEntity.ok(adminOrderService.overrideOrderStatus(id, trangThaiMoi, lyDo, authentication.getName()));
    }

    // 3. POST: Refund - Hoàn tiền
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> refundOrder(@PathVariable Long id,
                                         @RequestParam String lyDo,
                                         Authentication authentication) {
        return ResponseEntity.ok(adminOrderService.refundOrder(id, lyDo, authentication.getName()));
    }

    // 4. DELETE: Hủy đơn ép buộc & Rollback Kho Hàng
    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cancelAndRollback(@PathVariable Long id,
                                               @RequestParam String lyDo,
                                               Authentication authentication) {
        adminOrderService.cancelAndRollbackInventory(id, lyDo, authentication.getName());
        return ResponseEntity.ok("Đã hủy đơn hàng HD-" + id + " và hoàn trả số lượng về kho thành công!");
    }

    // 5. PUT: Chỉnh sửa thông tin đơn hàng (địa chỉ, SĐT, ngày giao, ghi chú)
    @PutMapping("/{id}/update")
    public ResponseEntity<?> updateOrderInfo(@PathVariable Long id,
                                             @Valid @RequestBody DatHangDto.UpdateRequest request,
                                             Authentication authentication) {
        return ResponseEntity.ok(adminOrderService.updateOrderInfo(id, request, authentication.getName()));
    }

    // 6. PUT: Đổi trạng thái theo flow chuẩn (có validate thứ tự)
    @PutMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable Long id,
                                          @RequestParam String trangThaiMoi,
                                          @RequestParam(required = false) String lyDoHuy,
                                          Authentication authentication) {
        return ResponseEntity.ok(adminOrderService.changeOrderStatus(id, trangThaiMoi, lyDoHuy, authentication.getName()));
    }

    // 7. GET: Lấy dữ liệu in đơn đầy đủ
    @GetMapping("/{id}/print")
    public ResponseEntity<?> getPrintData(@PathVariable Long id) {
        return ResponseEntity.ok(adminOrderService.getPrintData(id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // T080 – GHI CHÚ NỘI BỘ
    // Nhân viên thêm ghi chú nội bộ → Bếp thấy khi xem đơn hàng ở trang quản
    // trị, khách hàng KHÔNG thấy (ghi chú nội bộ không có trong API khách hàng).
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/{id}/internal-note")
    @Operation(
            summary = "Thêm/cập nhật ghi chú nội bộ",
            description = "Nhân viên hoặc Admin ghi chú nội bộ cho đơn hàng (VD: dặn dò bếp, lưu ý giao hàng...). " +
                    "Ghi chú này chỉ hiển thị cho Nhân viên/Admin qua trang quản trị, khách hàng không bao giờ thấy được."
    )
    public ResponseEntity<?> updateInternalNote(@PathVariable Long id,
                                                @RequestParam String ghiChuNoiBo,
                                                Authentication authentication) {
        return ResponseEntity.ok(adminOrderService.updateInternalNote(id, ghiChuNoiBo, authentication.getName()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // T080 – BARCODE GIAO HÀNG
    // Nhân viên giao hàng quét mã bill ("HD-{id}") lúc giao cho khách → đơn tự
    // động chuyển từ DANG_GIAO sang DA_GIAO, không cần đổi trạng thái thủ công.
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/scan-delivery")
    @Operation(
            summary = "Quét mã vạch/bill khi giao hàng",
            description = "Nhân viên giao hàng quét mã bill (mã đơn hàng dạng HD-{id}) khi giao tận nơi cho khách " +
                    "→ đơn tự động chuyển sang trạng thái DA_GIAO. Chỉ áp dụng cho đơn đang ở trạng thái DANG_GIAO."
    )
    public ResponseEntity<?> scanDelivery(@RequestParam String maVach, Authentication authentication) {
        return ResponseEntity.ok(adminOrderService.scanDelivery(maVach, authentication.getName()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // T056 – XÁC NHẬN THIẾT KẾ BÁNH 3D
    // Nhân viên nhấn "Xác nhận thiết kế" → Trừ kho phụ kiện + DANG_LAM + báo khách
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/{id}/confirm-design")
    @Operation(
            summary = "Xác nhận thiết kế bánh 3D",
            description = "Nhân viên xác nhận thiết kế → trừ tồn kho phụ kiện trang trí " +
                    "→ chuyển đơn sang DANG_LAM → thông báo khách hàng. " +
                    "Chỉ áp dụng cho đơn có thiết kế 3D và đang ở trạng thái DA_XAC_NHAN."
    )
    public ResponseEntity<?> confirmDesign(@PathVariable Long id,
                                           Authentication authentication) {
        return ResponseEntity.ok(adminOrderService.confirmDesign(id, authentication.getName()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // T057 – TỪ CHỐI THIẾT KẾ BÁNH 3D
    // Nhân viên nhấn "Từ chối" + nhập lý do → Đơn về CHO_XAC_NHAN + báo khách sửa lại
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/{id}/reject-design")
    @Operation(
            summary = "Từ chối thiết kế bánh 3D",
            description = "Nhân viên từ chối thiết kế + nhập lý do → đơn quay về CHO_XAC_NHAN " +
                    "→ thông báo khách sửa lại thiết kế. " +
                    "Chỉ áp dụng cho đơn có thiết kế 3D và đang ở trạng thái DA_XAC_NHAN."
    )
    public ResponseEntity<?> rejectDesign(@PathVariable Long id,
                                          @RequestParam String lyDo,
                                          Authentication authentication) {
        return ResponseEntity.ok(adminOrderService.rejectDesign(id, lyDo, authentication.getName()));
    }
}