package com.poly.cake.controller;

import com.poly.cake.dto.PhuKienTrangTriDto;
import com.poly.cake.service.PhuKienTrangTriService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * T050 - DecorPanel: API công khai cho khách hàng xem danh sách phụ kiện trang trí còn hàng.
 * GET /api/v1/accessories
 */
@RestController
@RequestMapping("/api/v1/accessories")
public class KhachHangPhuKienController {

    @Autowired
    private PhuKienTrangTriService phuKienTrangTriService;

    @GetMapping
    public ResponseEntity<List<PhuKienTrangTriDto.Response>> getPublicAccessories() {
        return ResponseEntity.ok(phuKienTrangTriService.getAvailableAccessories());
    }
}
