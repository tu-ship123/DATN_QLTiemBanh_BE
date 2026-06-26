package com.poly.cake.controller;

import com.poly.cake.dto.CakeDesignDto;
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
        try {
            String email = authentication.getName();
            OrderDto.Response response = orderService.createOrder(request, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getLocalizedMessage());
        }
    }

    // T055 – VALIDATE THIẾT KẾ 3D TRƯỚC KHI ĐẶT HÀNG
    // FE gọi ngay tại bước chọn khung để kiểm tra kích thước trước khi next step
    @PostMapping("/validate-cake-design")
    @PreAuthorize("hasRole('KHACH_HANG')")
    @Operation(summary = "Validate thiết kế bánh 3D",
            description = "FE gọi tại bước chọn khung để kiểm tra JSON hợp lệ chưa (kích thước chiều cao + đường kính)")
    public ResponseEntity<?> validateCakeDesign(@Valid @RequestBody CakeDesignDto.Request request) {
        try {
            CakeDesignDto.KichThuoc kt = request.getKhung().getKich_thuoc();
            return ResponseEntity.ok(Map.of(
                    "hopLe",   true,
                    "tomTat",  String.format("Đường kính %.0f cm × Chiều cao %.0f cm",
                                             kt.getDuong_kinh_cm(), kt.getChieu_cao_cm()),
                    "message", "Kích thước hợp lệ! Bạn có thể tiếp tục đặt hàng."
            ));
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

    // 3. API XEM CHI TIẾT ĐƠN HÀNG THEO ID
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

    // 5. API XỬ LÝ ĐƠN HÀNG - Chỉ ADMIN hoặc NHAN_VIEN có quyền thao tác
    @PutMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN')")
    public ResponseEntity<?> processOrder(@PathVariable Long id, @Valid @RequestBody OrderProcessDto request, Authentication authentication) {
        try {
            String emailNhanVien = authentication.getName();
            OrderDto.Response updatedOrder = orderService.processOrder(id, request, emailNhanVien);
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

    // 7. API LẤY DỮ LIỆU THIẾT KẾ 3D
    @GetMapping("/{id}/design")
    @PreAuthorize("hasAnyRole('NHAN_VIEN', 'ADMIN', 'KHACH_HANG')")
    @Operation(summary = "Lấy dữ liệu thiết kế 3D của đơn hàng",
            description = "Trả về cấu trúc JSON đầy đủ để Frontend render Three.js popup")
    public ResponseEntity<?> getOrder3DDesign(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.get3DCakeDesign(id));
    }
}