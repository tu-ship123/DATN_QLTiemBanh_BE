package com.poly.cake.controller;

import com.poly.cake.dto.OrderDto;
import com.poly.cake.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // 1. API ĐẶT HÀNG (CHECKOUT) - Chỉ dành cho Khách hàng
    @PostMapping
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<?> checkout(@RequestBody OrderDto.Request request, Authentication authentication) {
        try {
            String email = authentication.getName();
            OrderDto.Response response = orderService.createOrder(request, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getLocalizedMessage());
        }
    }

    // 2. API LẤY LỊCH SỬ ĐƠN HÀNG CỦA KHÁCH ĐANG LOG IN - Chỉ dành cho Khách hàng
    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<List<OrderDto.Response>> getMyOrders(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getOrdersByUser(email));
    }

    // 3. API XEM CHI TIẾT ĐƠN HÀNG THEO ID - Ai cũng có quyền xem (Khách xem đơn của họ, Admin/Staff xem để xử lý)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('KHACH_HANG', 'ADMIN', 'NHAN_VIEN')")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(orderService.getOrderById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 4. API LẤY TOÀN BỘ ĐƠN HÀNG - Chỉ ADMIN hoặc NHAN_VIEN mới được xem
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN')")
    public ResponseEntity<List<OrderDto.Response>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    // 5. API CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG - Chỉ ADMIN hoặc NHAN_VIEN có quyền duyệt
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            OrderDto.Response updatedOrder = orderService.updateStatus(id, status);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 6. API USER TỰ HỦY ĐƠN HÀNG - Chỉ Khách hàng mới được tự hủy đơn của mình
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, Authentication authentication) {
        try {
            String email = authentication.getName();
            orderService.cancelOrder(id, email);
            return ResponseEntity.ok("Hủy đơn hàng thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/{id}/design")
    @PreAuthorize("hasAnyAuthority('NHAN_VIEN', 'ADMIN', 'KHACH_HANG')")
    @Operation(summary = "Lấy dữ liệu thiết kế 3D của đơn hàng",
            description = "Trả về cấu trúc JSON đầy đủ để Frontend render Three.js popup")
    public ResponseEntity<?> getOrder3DDesign(@PathVariable Long id) {
        // Gọi hàm bên Service và trả về luôn
        return ResponseEntity.ok(orderService.get3DCakeDesign(id));
    }
}