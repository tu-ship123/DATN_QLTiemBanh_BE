package com.poly.cake.controller;

import com.poly.cake.exception.NgoaiLeKhongTimThayTaiNguyen;

import com.poly.cake.dto.CaiDatRequest;
import com.poly.cake.entity.CauHinhHeThong;
import com.poly.cake.entity.NhatKyHeThong;
import com.poly.cake.repository.CauHinhHeThongRepository;
import com.poly.cake.repository.NhatKyHeThongRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/settings")

public class CaiDatController {

    @Autowired
    private CauHinhHeThongRepository cauHinhHeThongRepository;

    @Autowired
    private NhatKyHeThongRepository nhatKyHeThongRepository; // Gọi thêm kho chứa Nhật ký

    @GetMapping
    public ResponseEntity<?> getAllSettings() {
        return ResponseEntity.ok(cauHinhHeThongRepository.findAll());
    }

    @PutMapping("/{khoaCauHinh}")
    public ResponseEntity<?> updateSetting(@PathVariable String khoaCauHinh, @Valid @RequestBody CaiDatRequest request) {
        CauHinhHeThong config = cauHinhHeThongRepository.findByKhoaCauHinh(khoaCauHinh)
                .orElse(null);

        String hanhDong = "UPDATE";
        String giaTriCu = null;

        if (config == null) {
            // Tự động tạo cấu hình nếu chưa tồn tại
            config = new CauHinhHeThong();
            config.setKhoaCauHinh(khoaCauHinh);
            hanhDong = "CREATE";
            giaTriCu = "{}"; // Không có giá trị cũ
        } else {
            // Lưu lại giá trị cũ trước khi sửa
            giaTriCu = "{\"giaTri\": \"" + config.getGiaTri() + "\"}";
        }

        // Cập nhật giá trị mới
        config.setGiaTri(request.getGiaTri());
        CauHinhHeThong savedConfig = cauHinhHeThongRepository.save(config);

        // Ghi vào sổ Nhật ký hệ thống
        NhatKyHeThong log = new NhatKyHeThong();
        log.setHanhDong(hanhDong);
        log.setTenBang("CAU_HINH_HE_THONG");
        log.setBanGhiId(savedConfig.getId());
        log.setGiaTriCu(giaTriCu);
        log.setGiaTriMoi("{\"giaTri\": \"" + savedConfig.getGiaTri() + "\"}");
        nhatKyHeThongRepository.save(log);

        return ResponseEntity.ok(savedConfig);
    }
}