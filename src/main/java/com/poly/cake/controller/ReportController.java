package com.poly.cake.controller;
import com.poly.cake.service.ReportService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.ByteArrayInputStream;
import java.util.Map;



@RestController
@RequestMapping("/api/reports")
// LƯU Ý BẢO MẬT: path "/api/reports/**" không khớp rule "/api/v1/admin/**" trong
// SecurityConfig -> nếu thiếu @PreAuthorize thì bất kỳ user đã đăng nhập nào (kể cả
// khách hàng) cũng xem được doanh thu, bảng lương nhân viên, đối soát giao dịch.
@PreAuthorize("hasRole('ADMIN')")
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
    // 1. API Hiệu suất nhân viên
    @GetMapping("/hieu-suat")
    public ResponseEntity<?> getHieuSuatNhanVien() {
        return ResponseEntity.ok(reportService.getHieuSuatNhanVien());
    }

    // 2. API Đối soát giao dịch (Hỗ trợ tìm theo mã ví dụ: ?maGiaoDich=VNP2026...)
    @GetMapping("/doi-soat")
    public ResponseEntity<?> getDoiSoatGiaoDich(@RequestParam(required = false) String maGiaoDich) {
        return ResponseEntity.ok(reportService.getDanhSachDoiSoat(maGiaoDich));
    }
    // API Xem bảng lương JSON
    @GetMapping("/luong")
    public ResponseEntity<?> getBangLuong(@RequestParam int thang, @RequestParam int nam) {
        return ResponseEntity.ok(reportService.tinhLuongNhanVien(thang, nam));
    }

    // API Tải file Excel Bảng lương
    @GetMapping("/luong/export-excel")
    public ResponseEntity<InputStreamResource> exportBangLuongExcel(@RequestParam int thang, @RequestParam int nam) throws Exception {
        ByteArrayInputStream in = reportService.exportLuongExcel(thang, nam);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=BangLuong_T" + thang + "_" + nam + ".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
    // API Xem danh sách sử dụng Voucher
    @GetMapping("/voucher-usage")
    public ResponseEntity<?> getVoucherUsage(@RequestParam(required = false) String maCode) {
        return ResponseEntity.ok(reportService.getVoucherUsage(maCode));
    }
}