package com.poly.cake.controller;

import com.poly.cake.repository.NhatKyHeThongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    @Autowired
    private NhatKyHeThongRepository nhatKyHeThongRepository;

    @GetMapping
    public ResponseEntity<?> getSensitiveLogs() {
        // Lọc diff JSON Old/New cho các thao tác UPDATE, DELETE
        return ResponseEntity.ok(nhatKyHeThongRepository.findByHanhDongInOrderByNgayTaoDesc(
                Arrays.asList("UPDATE", "DELETE", "SOFT_DELETE")
        ));
    }
}