package com.poly.cake.controller;

import com.poly.cake.entity.NhatKyHeThong;
import com.poly.cake.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Path "/api/v1/admin/**" đã được bảo vệ ROLE_ADMIN sẵn trong SecurityConfig
@RestController
@RequestMapping("/api/v1/admin/backup")
@RequiredArgsConstructor
public class AdminBackupController {

    private final BackupService backupService;

    // Bấm nút "Backup ngay" trên trang Admin > Backup
    @PostMapping("/run")
    public ResponseEntity<NhatKyHeThong> runBackup(Authentication authentication) {
        return ResponseEntity.ok(backupService.backupNgay(authentication.getName()));
    }

    // Lịch sử các lần backup (tự động lúc 2h sáng + thủ công)
    @GetMapping("/history")
    public ResponseEntity<List<NhatKyHeThong>> getHistory() {
        return ResponseEntity.ok(backupService.getLichSuBackup());
    }
}
