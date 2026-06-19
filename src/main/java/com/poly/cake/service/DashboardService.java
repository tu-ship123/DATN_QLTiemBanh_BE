package com.poly.cake.service;

import com.poly.cake.repository.DonHangRepository;
import com.poly.cake.repository.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    // 1. API LẤY KPI DASHBOARD
    public Map<String, Object> getDashboardKpi() {
        // Mốc thời gian Hôm nay
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = startOfToday.plusDays(1);

        // Mốc thời gian Hôm qua
        LocalDateTime startOfYesterday = startOfToday.minusDays(1);
        LocalDateTime endOfYesterday = startOfToday;

        // Lấy Data Hôm nay
        BigDecimal doanhThuHnay = donHangRepository.sumDoanhThuByDateRange(startOfToday, endOfToday);
        Long donHnay = donHangRepository.countDonHangByDateRange(startOfToday, endOfToday);
        Long khachMoiHnay = nguoiDungRepository.countKhachMoiByDateRange(startOfToday, endOfToday);

        // Lấy Data Hôm qua
        BigDecimal doanhThuHqua = donHangRepository.sumDoanhThuByDateRange(startOfYesterday, endOfYesterday);
        Long donHqua = donHangRepository.countDonHangByDateRange(startOfYesterday, endOfYesterday);

        // Tính % tăng trưởng (Tăng/Giảm so với hôm qua)
        double phanTramDoanhThu = calculateGrowth(doanhThuHqua.doubleValue(), doanhThuHnay.doubleValue());
        double phanTramDon = calculateGrowth(donHqua.doubleValue(), donHnay.doubleValue());

        // Đóng gói trả về
        Map<String, Object> result = new HashMap<>();
        result.put("tongDoanhThu", doanhThuHnay);
        result.put("tongDon", donHnay);
        result.put("khachMoi", khachMoiHnay);
        result.put("tangTruongDoanhThu", phanTramDoanhThu);
        result.put("tangTruongDon", phanTramDon);

        return result;
    }

    // 2. API LẤY BÁO CÁO DOANH THU THEO NGÀY
    public List<Map<String, Object>> getRevenueReport() {
        List<Object[]> rawData = donHangRepository.getRevenueReportByDay();
        List<Map<String, Object>> report = new ArrayList<>();

        for (Object[] row : rawData) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", row[0].toString()); // CAST AS DATE trả về chuỗi YYYY-MM-DD
            item.put("revenue", row[1]);
            report.add(item);
        }
        return report;
    }

    // Hàm phụ trợ tính % tăng trưởng an toàn
    private double calculateGrowth(double oldVal, double newVal) {
        if (oldVal == 0) return newVal > 0 ? 100.0 : 0.0;
        return Math.round(((newVal - oldVal) / oldVal) * 100.0 * 100.0) / 100.0; // Làm tròn 2 chữ số
    }
}