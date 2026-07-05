package com.poly.cake.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.poly.cake.dto.SanPhamDto;
import com.poly.cake.service.AdminSanPhamService;
import com.poly.cake.entity.SanPham;

import java.util.Map;

import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/admin/products")
// 👉 BẠN XÓA HẲN DÒNG @PreAuthorize... Ở VỊ TRÍ NÀY ĐI NHÉ
public class AdminSanPhamController {
    // ...


    @Autowired
    private AdminSanPhamService adminSanPhamService;

    @Autowired
    private com.poly.cake.service.InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<?> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) Long danhMucId) {

        return ResponseEntity.ok(
                adminSanPhamService.getFilteredProducts(
                        keyword,
                        trangThai,
                        danhMucId
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                adminSanPhamService.getProductById(id)
        );
    }

    // Danh sách sản phẩm đang tồn kho thấp (so_luong_ton <= nguong_canh_bao)
    @GetMapping("/canh-bao-ton-thap")
    public ResponseEntity<?> getLowStockProducts() {
        return ResponseEntity.ok(
                adminSanPhamService.getLowStockProducts()
        );
    }

    // Điều chỉnh tồn kho thủ công (nhập hàng, kiểm kê, hoặc TEST cảnh báo qua Postman).
    // body: { "soLuongThayDoi": -6 }  -> âm = trừ bớt, dương = nhập thêm
    // Tự động kiểm tra ngưỡng và gửi cảnh báo (lưu DB + báo real-time) nếu tồn kho xuống thấp.
    @PatchMapping("/{id}/dieu-chinh-ton-kho")
    public ResponseEntity<?> dieuChinhTonKho(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {

        int soLuongThayDoi = body.getOrDefault("soLuongThayDoi", 0);
        SanPham sanPham = inventoryService.dieuChinhTonKhoThuCong(id, soLuongThayDoi);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "id", sanPham.getId(),
                "tenSanPham", sanPham.getTenSanPham(),
                "soLuongTon", sanPham.getSoLuongTon(),
                "nguongCanhBao", sanPham.getNguongCanhBao()
        ));
    }

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody SanPhamDto.Request request) {

        return ResponseEntity.ok(
                adminSanPhamService.createProduct(request)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody SanPhamDto.Request request) {

        return ResponseEntity.ok(
                adminSanPhamService.updateProduct(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {

        adminSanPhamService.deleteProduct(id);

        return ResponseEntity.ok("Xóa sản phẩm thành công");
    }
}