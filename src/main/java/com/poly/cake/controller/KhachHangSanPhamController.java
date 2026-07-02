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
     * API công khai - lấy (hoặc tự tạo nếu chưa có) sản phẩm đại diện dùng chung cho
     * mọi chiếc bánh khách tự thiết kế ở CakeBuilder3D. FE gọi ngay trước khi thêm
     * bánh 3D vào giỏ hàng (xem Design.vue -> datBanhNay()).
     * GET /api/v1/products/custom-cake-marker
     */
    @GetMapping("/custom-cake-marker")
    public ResponseEntity<SanPhamDto.Response> getCustomCakeMarker() {
        return ResponseEntity.ok(adminSanPhamService.getOrCreateCustomCakeMarker());
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