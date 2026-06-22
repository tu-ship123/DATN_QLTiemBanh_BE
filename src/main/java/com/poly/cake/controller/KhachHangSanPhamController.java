package com.poly.cake.controller;

import com.poly.cake.dto.SanPhamDto;
import com.poly.cake.service.AdminSanPhamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class KhachHangSanPhamController {

    @Autowired
    private AdminSanPhamService adminSanPhamService;

    /**
     * API công khai - khách hàng xem danh sách sản phẩm đang bán
     * GET /api/v1/products
     * GET /api/v1/products?keyword=bánh
     * GET /api/v1/products?danhMucId=1
     * GET /api/v1/products?keyword=bánh&danhMucId=1
     */
    @GetMapping
    public ResponseEntity<List<SanPhamDto.Response>> getPublicProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long danhMucId) {

        // Chỉ lấy sản phẩm có trạng thái "DANG_BAN"
        List<SanPhamDto.Response> products =
                adminSanPhamService.getFilteredProducts(keyword, "DANG_BAN", danhMucId);

        return ResponseEntity.ok(products);
    }

    /**
     * API công khai - xem chi tiết 1 sản phẩm
     * GET /api/v1/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<SanPhamDto.Response> getProductDetail(
            @PathVariable Long id) {

        SanPhamDto.Response product =
                adminSanPhamService.getProductById(id);

        return ResponseEntity.ok(product);
    }
}