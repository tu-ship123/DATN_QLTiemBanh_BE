package com.poly.cake.controller;

import com.poly.cake.dto.ThietKeYeuThichDto;
import com.poly.cake.entity.ThietKeYeuThich;
import com.poly.cake.service.ThietKeYeuThichService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Danh sách thiết kế bánh 3D khách hàng đã lưu ("yêu thích", xem lại ở
 * WishlistPage.vue). Đổi path sang /api/v1/wishlist/thiet-ke để đồng bộ tiền
 * tố /api/v1 với các API khách hàng khác, và FIX MOCK_USER_ID = 1L (khiến
 * mọi khách hàng dùng chung 1 danh sách yêu thích) bằng người dùng thật lấy
 * từ Authentication.
 */
@RestController
@RequestMapping("/api/v1/wishlist/thiet-ke")
@CrossOrigin("*")
@PreAuthorize("isAuthenticated()")
public class ThietKeYeuThichController {

    @Autowired
    private ThietKeYeuThichService yeuThichService;

    @GetMapping
    public ResponseEntity<List<ThietKeYeuThich>> getAll(Authentication authentication) {
        return ResponseEntity.ok(yeuThichService.getDanhSachYeuThich(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<ThietKeYeuThich> create(Authentication authentication, @RequestBody ThietKeYeuThichDto request) {
        return ResponseEntity.ok(yeuThichService.luuThietKe(authentication.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(Authentication authentication, @PathVariable Long id) {
        yeuThichService.xoaThietKe(authentication.getName(), id);
        return ResponseEntity.ok("Đã xóa khỏi danh sách yêu thích!");
    }
}
