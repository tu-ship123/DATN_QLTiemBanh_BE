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
        try {
            // Lấy email của Nhân viên thu ngân đang thực hiện đăng nhập từ Token
            String emailNhanVien = authentication.getName();
            
            PosOrderDto.Response response = posOrderService.createPosOrder(request, emailNhanVien);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}