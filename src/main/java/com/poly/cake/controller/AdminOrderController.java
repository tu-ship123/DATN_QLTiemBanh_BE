package com.poly.cake.controller;

import com.poly.cake.dto.OrderDto;
import com.poly.cake.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
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
public class AdminOrderController {

    @Autowired
    private AdminOrderService adminOrderService;

    // 1. GET: Lọc đơn hàng nâng cao (Dùng param trên URL)
    @GetMapping
    public ResponseEntity<List<OrderDto.Response>> filterOrders(
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
        try {
            return ResponseEntity.ok(adminOrderService.overrideOrderStatus(id, trangThaiMoi, lyDo, authentication.getName()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 3. POST: Refund - Hoàn tiền
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> refundOrder(@PathVariable Long id,
                                         @RequestParam String lyDo,
                                         Authentication authentication) {
        try {
            return ResponseEntity.ok(adminOrderService.refundOrder(id, lyDo, authentication.getName()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 4. DELETE: Hủy đơn ép buộc & Rollback Kho Hàng
    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cancelAndRollback(@PathVariable Long id,
                                               @RequestParam String lyDo,
                                               Authentication authentication) {
        try {
            adminOrderService.cancelAndRollbackInventory(id, lyDo, authentication.getName());
            return ResponseEntity.ok("Đã hủy đơn hàng HD-" + id + " và hoàn trả số lượng về kho thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 5. PUT: Chỉnh sửa thông tin đơn hàng (địa chỉ, SĐT, ngày giao, ghi chú)
    @PutMapping("/{id}/update")
    public ResponseEntity<?> updateOrderInfo(@PathVariable Long id,
                                             @RequestBody OrderDto.UpdateRequest request,
                                             Authentication authentication) {
        try {
            return ResponseEntity.ok(adminOrderService.updateOrderInfo(id, request, authentication.getName()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 6. PUT: Đổi trạng thái theo flow chuẩn (có validate thứ tự)
    @PutMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable Long id,
                                          @RequestParam String trangThaiMoi,
                                          @RequestParam(required = false) String lyDoHuy,
                                          Authentication authentication) {
        try {
            return ResponseEntity.ok(adminOrderService.changeOrderStatus(id, trangThaiMoi, lyDoHuy, authentication.getName()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 7. GET: Lấy dữ liệu in đơn đầy đủ
    @GetMapping("/{id}/print")
    public ResponseEntity<?> getPrintData(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminOrderService.getPrintData(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
        try {
            return ResponseEntity.ok(adminOrderService.confirmDesign(id, authentication.getName()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}