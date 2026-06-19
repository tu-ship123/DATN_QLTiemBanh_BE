package com.poly.cake.controller;

import com.poly.cake.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    // 1. Lấy KPI tổng quan
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardKpi());
    }

    // 2. Lấy data vẽ biểu đồ doanh thu
    @GetMapping("/reports/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRevenueReport() {
        return ResponseEntity.ok(dashboardService.getRevenueReport());
    }
}