package com.poly.cake.controller;

import com.poly.cake.dto.PosOrderDto;
import com.poly.cake.service.PosOrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pos/orders")
public class PosOrderController {

    @Autowired
    private PosOrderService posOrderService;

    // API Tạo hóa đơn tại quầy + Lấy mã chuyển khoản VietQR + Dữ liệu in bill
    @PostMapping
    public ResponseEntity<?> createPosOrder(@Valid @RequestBody PosOrderDto.Request request, Authentication authentication) {
        // Lấy email của Nhân viên thu ngân đang thực hiện đăng nhập từ Token
        String emailNhanVien = authentication.getName();

        PosOrderDto.Response response = posOrderService.createPosOrder(request, emailNhanVien);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // API Xác nhận đã nhận tiền QR cho hóa đơn quầy -> chuyển thẳng HOAN_THANH (mua tại quầy, không qua sản xuất)
    @PutMapping("/{id}/confirm-paid")
    public ResponseEntity<?> confirmPosOrderPaid(@PathVariable Long id, Authentication authentication) {
        String emailNhanVien = authentication.getName();
        posOrderService.confirmPosOrderPaid(id, emailNhanVien);
        return ResponseEntity.ok("Đã xác nhận thanh toán, hóa đơn hoàn tất!");
    }

    // API Hủy hóa đơn tại quầy khi nhân viên bấm "Huỷ" / đóng mã QR trước khi khách thanh toán xong
    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<?> cancelPosOrder(@PathVariable Long id, Authentication authentication) {
        String emailNhanVien = authentication.getName();
        posOrderService.cancelPosOrder(id, emailNhanVien);
        return ResponseEntity.ok("Đã hủy hóa đơn POS thành công!");
    }
}