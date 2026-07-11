package com.poly.cake.controller;

import com.poly.cake.dto.DiaChiDto;
import com.poly.cake.entity.DiaChiGiaoHang;
import com.poly.cake.service.DiaChiGiaoHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dia-chi")
@CrossOrigin("*")
public class DiaChiGiaoHangController {

    @Autowired
    private DiaChiGiaoHangService diaChiService;

    // Giả lập lấy ID từ Security Context, sau này sẽ thay bằng Principal
    private final Long MOCK_USER_ID = 1L;

    @GetMapping
    public ResponseEntity<List<DiaChiGiaoHang>> getAll() {
        return ResponseEntity.ok(diaChiService.getDanhSachDiaChi(MOCK_USER_ID));
    }

    @PostMapping
    public ResponseEntity<DiaChiGiaoHang> create(@RequestBody DiaChiDto request) {
        return ResponseEntity.ok(diaChiService.themDiaChi(MOCK_USER_ID, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiaChiGiaoHang> update(@PathVariable Long id, @RequestBody DiaChiDto request) {
        return ResponseEntity.ok(diaChiService.capNhatDiaChi(MOCK_USER_ID, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        diaChiService.xoaDiaChi(MOCK_USER_ID, id);
        return ResponseEntity.ok("Xóa địa chỉ thành công!");
    }
}