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
     * TASK 1: Khách hàng xem danh sách sản phẩm đang bán (Hỗ trợ tìm kiếm, lọc danh mục và sắp xếp giá)
     * GET /api/v1/products?sort=asc              -> Thấp đến cao
     * GET /api/v1/products?sort=desc             -> Cao đến thấp
     * GET /api/v1/products?keyword=cake&sort=asc -> Tìm bánh theo từ khóa + sắp xếp giá
     */
    @GetMapping
    public ResponseEntity<List<SanPhamDto.Response>> getPublicProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long danhMucId,
            @RequestParam(required = false) String sort) { // Nhận thêm tham số sort (asc/desc)

        // Nếu FE truyền tham số sort, gọi hàm sắp xếp mới tích hợp lọc
        if (sort != null && (sort.equalsIgnoreCase("asc") || sort.equalsIgnoreCase("desc"))) {
            List<SanPhamDto.Response> sortedProducts = 
                    adminSanPhamService.getPublicProductsWithSort(keyword, danhMucId, sort);
            return ResponseEntity.ok(sortedProducts);
        }

        // Nếu FE không truyền sort, chạy lại logic lọc cũ theo ngày tạo giảm dần của nhóm bạn
        List<SanPhamDto.Response> products =
                adminSanPhamService.getFilteredProducts(keyword, "DANG_BAN", danhMucId);
                
        return ResponseEntity.ok(products);
    }

    /**
     * API công khai - Lấy sản phẩm mẫu cho bánh tự thiết kế 3D
     * GET /api/v1/products/custom-cake-marker
     */
    @GetMapping("/custom-cake-marker")
    public ResponseEntity<SanPhamDto.Response> getCustomCakeMarker() {
        return ResponseEntity.ok(adminSanPhamService.getOrCreateCustomCakeMarker());
    }

    /**
     * TASK 2: Xem chi tiết 1 sản phẩm -> Trả về sản phẩm đó kèm danh sách 4 sản phẩm cùng loại gợi ý
     * GET /api/v1/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<SanPhamDto.DetailResponse> getProductDetail(@PathVariable Long id) {
        
        // Gọi hàm tích hợp gợi ý từ adminSanPhamService
        SanPhamDto.DetailResponse detailWithSuggestions = 
                adminSanPhamService.getProductDetailWithSuggestions(id);
                
        return ResponseEntity.ok(detailWithSuggestions);
    }
}