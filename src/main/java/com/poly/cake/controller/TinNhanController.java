package com.poly.cake.controller;

import com.poly.cake.entity.NguoiDung;
import com.poly.cake.exception.NgoaiLeKhongTimThayTaiNguyen;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.service.TinNhanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API cho khách hàng nhắn tin với cửa hàng (đối xứng với AdminTinNhanController).
 * Chỉ thao tác trên hội thoại của CHÍNH người đang đăng nhập -> không nhận
 * khachHangId từ request để tránh xem/gửi tin thay người khác.
 */
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TinNhanController {

    private final TinNhanService messageService;
    private final NguoiDungRepository nguoiDungRepository;

    private NguoiDung currentUser(Authentication authentication) {
        return nguoiDungRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen("Tài khoản không tồn tại!"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyMessages(Authentication authentication) {
        NguoiDung me = currentUser(authentication);
        return ResponseEntity.ok(messageService.getMessages(me.getId()));
    }

    @PostMapping("/me")
    public ResponseEntity<?> sendMessage(Authentication authentication, @RequestBody Map<String, String> body) {
        NguoiDung me = currentUser(authentication);
        return ResponseEntity.ok(messageService.guiTinNhan(me.getId(), me.getId(), body.get("noiDung"), false));
    }
}
