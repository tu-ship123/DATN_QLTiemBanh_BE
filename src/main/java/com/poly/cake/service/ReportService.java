package com.poly.cake.service;

import com.poly.cake.dto.DoanhThuKenhDto;
import com.poly.cake.dto.DoiSoatDto;
import com.poly.cake.dto.HieuSuatNhanVienDto;
import com.poly.cake.dto.TopSanPhamDto;
import com.poly.cake.repository.DonHangRepository;
import com.poly.cake.repository.ThanhToanRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ReportService {

    private final DonHangRepository donHangRepository;
    private final ThanhToanRepository thanhToanRepository;

    // Inject cả 2 Repository vào chung 1 Constructor
    public ReportService(DonHangRepository donHangRepository, ThanhToanRepository thanhToanRepository) {
        this.donHangRepository = donHangRepository;
        this.thanhToanRepository = thanhToanRepository;
    }

    // ==========================================
    // TASK T082: BÁO CÁO DOANH THU & TOP SẢN PHẨM
    // ==========================================

    public List<DoanhThuKenhDto> getDoanhThuKenh() {
        return donHangRepository.getDoanhThuTheoKenh();
    }

    public List<TopSanPhamDto> getTopSanPham() {
        // Lấy Top 10 sản phẩm bán chạy nhất
        return donHangRepository.getTopSanPhamBanChay(PageRequest.of(0, 10));
    }

    // Code Generate file Excel
    public ByteArrayInputStream exportReportToExcel() throws IOException {
        List<DoanhThuKenhDto> doanhThuList = getDoanhThuKenh();
        List<TopSanPhamDto> topSanPhamList = getTopSanPham();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // --- SHEET 1: DOANH THU KÊNH ---
            Sheet sheet1 = workbook.createSheet("Doanh Thu Kênh");
            Row header1 = sheet1.createRow(0);
            header1.createCell(0).setCellValue("Kênh Bán (Nguồn Đơn)");
            header1.createCell(1).setCellValue("Tổng Doanh Thu (VNĐ)");

            int rowIdx1 = 1;
            for (DoanhThuKenhDto dt : doanhThuList) {
                Row row = sheet1.createRow(rowIdx1++);
                row.createCell(0).setCellValue(dt.getNguonDon());
                row.createCell(1).setCellValue(dt.getTongDoanhThu().doubleValue());
            }

            // --- SHEET 2: TOP SẢN PHẨM ---
            Sheet sheet2 = workbook.createSheet("Top Sản Phẩm Bán Chạy");
            Row header2 = sheet2.createRow(0);
            header2.createCell(0).setCellValue("Tên Sản Phẩm");
            header2.createCell(1).setCellValue("Số Lượng Bán Ra");

            int rowIdx2 = 1;
            for (TopSanPhamDto sp : topSanPhamList) {
                Row row = sheet2.createRow(rowIdx2++);
                row.createCell(0).setCellValue(sp.getTenSanPham());
                row.createCell(1).setCellValue(sp.getTongSoLuongBan());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // ==========================================
    // TASK T087: ĐỐI SOÁT GIAO DỊCH & HIỆU SUẤT
    // ==========================================

    public List<HieuSuatNhanVienDto> getHieuSuatNhanVien() {
        return donHangRepository.getHieuSuatNhanVien();
    }

    public List<DoiSoatDto> getDanhSachDoiSoat(String maGiaoDich) {
        return thanhToanRepository.getDanhSachDoiSoat(maGiaoDich);
    }
}