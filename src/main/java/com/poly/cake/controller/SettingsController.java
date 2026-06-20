package com.poly.cake.controller;

import com.poly.cake.dto.SettingRequest;
import com.poly.cake.entity.CauHinhHeThong;
import com.poly.cake.entity.NhatKyHeThong;
import com.poly.cake.repository.CauHinhHeThongRepository;
import com.poly.cake.repository.NhatKyHeThongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {

    @Autowired
    private CauHinhHeThongRepository cauHinhHeThongRepository;

    @Autowired
    private NhatKyHeThongRepository nhatKyHeThongRepository; // Gọi thêm kho chứa Nhật ký

    @GetMapping
    public ResponseEntity<?> getAllSettings() {
        return ResponseEntity.ok(cauHinhHeThongRepository.findAll());
    }

    @PutMapping("/{khoaCauHinh}")
    public ResponseEntity<?> updateSetting(@PathVariable String khoaCauHinh, @RequestBody SettingRequest request) {
        CauHinhHeThong config = cauHinhHeThongRepository.findByKhoaCauHinh(khoaCauHinh)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình: " + khoaCauHinh));

        // 1. Lưu lại giá trị cũ trước khi sửa
        String giaTriCu = config.getGiaTri();

        // 2. Cập nhật giá trị mới
        config.setGiaTri(request.getGiaTri());
        CauHinhHeThong savedConfig = cauHinhHeThongRepository.save(config);

        // 3. Ghi vào sổ Nhật ký hệ thống (Diff JSON Old/New đúng như thẻ Trello yêu cầu)
        NhatKyHeThong log = new NhatKyHeThong();
        log.setHanhDong("UPDATE");
        log.setTenBang("CAU_HINH_HE_THONG");
        log.setBanGhiId(savedConfig.getId());
        log.setGiaTriCu("{\"giaTri\": \"" + giaTriCu + "\"}");
        log.setGiaTriMoi("{\"giaTri\": \"" + savedConfig.getGiaTri() + "\"}");
        nhatKyHeThongRepository.save(log);

        return ResponseEntity.ok(savedConfig);
    }
}