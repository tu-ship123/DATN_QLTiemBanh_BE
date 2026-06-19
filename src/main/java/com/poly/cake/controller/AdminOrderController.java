package com.poly.cake.controller;

import com.poly.cake.dto.OrderDto;
import com.poly.cake.service.AdminOrderService;
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
@PreAuthorize("hasRole('ADMIN')") // Chặn tuyệt đối, chỉ Admin mới được dùng
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
}