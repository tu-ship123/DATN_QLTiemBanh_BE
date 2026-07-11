package com.poly.cake.controller;

import com.poly.cake.dto.ThietKeYeuThichDto;
import com.poly.cake.entity.ThietKeYeuThich;
import com.poly.cake.service.ThietKeYeuThichService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/thiet-ke-yeu-thich")
@CrossOrigin("*")
public class ThietKeYeuThichController {

    @Autowired
    private ThietKeYeuThichService yeuThichService;

    private final Long MOCK_USER_ID = 1L;

    @GetMapping
    public ResponseEntity<List<ThietKeYeuThich>> getAll() {
        return ResponseEntity.ok(yeuThichService.getDanhSachYeuThich(MOCK_USER_ID));
    }

    @PostMapping
    public ResponseEntity<ThietKeYeuThich> create(@RequestBody ThietKeYeuThichDto request) {
        return ResponseEntity.ok(yeuThichService.luuThietKe(MOCK_USER_ID, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        yeuThichService.xoaThietKe(MOCK_USER_ID, id);
        return ResponseEntity.ok("Đã xóa khỏi danh sách yêu thích!");
    }
}