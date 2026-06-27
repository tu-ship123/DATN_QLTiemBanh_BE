package com.poly.cake.controller;

import com.poly.cake.dto.OrderDto;
import com.poly.cake.dto.OrderProcessDto;
import com.poly.cake.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // 1. API ĐẶT HÀNG (CHECKOUT) - Chỉ dành cho Khách hàng
    @PostMapping
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<?> checkout(@Valid @RequestBody OrderDto.Request request, Authentication authentication) {
        String email = authentication.getName();
        OrderDto.Response response = orderService.createOrder(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. API LẤY LỊCH SỬ ĐƠN HÀNG CỦA KHÁCH ĐANG LOG IN - Chỉ dành cho Khách hàng
    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<List<OrderDto.Response>> getMyOrders(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getOrdersByUser(email));
    }

    // 3. API XEM CHI TIẾT ĐƠN HÀNG THEO ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('KHACH_HANG', 'ADMIN', 'NHAN_VIEN')")
    public ResponseEntity<?> getOrderById(@PathVariable Long id, Authentication authentication) {
        // Lấy email và quyền của người đang đăng nhập
        String email = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // Truyền thêm email và quyền xuống Service để kiểm tra
        return ResponseEntity.ok(orderService.getOrderById(id, email, role));
    }

    // 4. API LẤY TOÀN BỘ ĐƠN HÀNG - Chỉ ADMIN hoặc NHAN_VIEN mới được xem
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN')")
    public ResponseEntity<List<OrderDto.Response>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    // 5. API XỬ LÝ ĐƠN HÀNG - Chỉ ADMIN hoặc NHAN_VIEN có quyền thao tác (Gửi body JSON)
    @PutMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN')")
    public ResponseEntity<?> processOrder(@PathVariable Long id, @Valid @RequestBody OrderProcessDto request, Authentication authentication) {
        // Lấy email của Admin/Nhân viên đang log in thao tác
        String emailNhanVien = authentication.getName();

        OrderDto.Response updatedOrder = orderService.processOrder(id, request, emailNhanVien);
        return ResponseEntity.ok(updatedOrder);
    }

    // 6. API USER TỰ HỦY ĐƠN HÀNG - Chỉ Khách hàng mới được tự hủy đơn của mình
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        orderService.cancelOrder(id, email);

        // Đã bọc String thô vào Map để Spring Boot tự động chuyển thành JSON Object
        return ResponseEntity.ok(Map.of("message", "Hủy đơn hàng thành công!"));
    }

    // 7. API LẤY DỮ LIỆU THIẾT KẾ 3D
    @GetMapping("/{id}/design")
    @PreAuthorize("hasAnyRole('NHAN_VIEN', 'ADMIN', 'KHACH_HANG')")
    @Operation(summary = "Lấy dữ liệu thiết kế 3D của đơn hàng",
            description = "Trả về cấu trúc JSON đầy đủ để Frontend render Three.js popup")
    public ResponseEntity<?> getOrder3DDesign(@PathVariable Long id) {
        // Gọi hàm bên Service và trả về luôn
        return ResponseEntity.ok(orderService.get3DCakeDesign(id));
    }
}