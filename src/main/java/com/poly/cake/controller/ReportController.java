package com.poly.cake.controller;
import com.poly.cake.service.ReportService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.ByteArrayInputStream;
import java.util.Map;


@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // 1. API Trả về JSON cho Vue.js vẽ biểu đồ
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData() {
        return ResponseEntity.ok(Map.of(
                "doanhThuKenh", reportService.getDoanhThuKenh(),
                "topSanPham", reportService.getTopSanPham()
        ));
    }

    // 2. API Tải file Excel
    @GetMapping("/export-excel")
    public ResponseEntity<InputStreamResource> exportExcel() throws Exception {
        ByteArrayInputStream in = reportService.exportReportToExcel();
        HttpHeaders headers = new HttpHeaders();
        // Thiết lập tên file tải xuống
        headers.add("Content-Disposition", "attachment; filename=BaoCaoDoanhThu.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}