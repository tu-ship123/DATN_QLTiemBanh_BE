package com.poly.cake.service;

import com.poly.cake.dto.*;
import com.poly.cake.repository.DonHangRepository;
import com.poly.cake.repository.PhieuKiemKeRepository;
import com.poly.cake.repository.ThanhToanRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import com.poly.cake.repository.ChamCongRepository;
import com.poly.cake.entity.ChamCong;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
@Service
public class ReportService {

    private final DonHangRepository donHangRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final ChamCongRepository chamCongRepository; // Thêm dòng này
    

    // Cập nhật Constructor để nhận đủ 3 Repository
    public ReportService(DonHangRepository donHangRepository,
                         ThanhToanRepository thanhToanRepository,
                         ChamCongRepository chamCongRepository) {
        this.donHangRepository = donHangRepository;
        this.thanhToanRepository = thanhToanRepository;
        this.chamCongRepository = chamCongRepository;
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
    // TASK T089: TÍNH LƯƠNG & XUẤT EXCEL
    // ==========================================

    public List<BangLuongDto> tinhLuongNhanVien(int thang, int nam) {
        List<ChamCong> danhSachChamCong = chamCongRepository.findByThangAndNam(thang, nam);
        Map<Long, BangLuongDto> mapLuong = new HashMap<>();

        // 1. Gom nhóm và tính tổng giờ làm, phút trễ
        for (ChamCong cc : danhSachChamCong) {
            Long nvId = cc.getPhanCa().getNhanVien().getId();
            String tenNv = cc.getPhanCa().getNhanVien().getHoTen();

            BangLuongDto dto = mapLuong.getOrDefault(nvId, new BangLuongDto(nvId, tenNv, 0.0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

            // Tính số phút làm việc thực tế
            long minutes = Duration.between(cc.getGioVao(), cc.getGioRa()).toMinutes();
            double gioLam = minutes / 60.0;
            dto.setTongGioLam(dto.getTongGioLam() + gioLam);

            if (cc.getPhutDiTre() != null) {
                dto.setTongPhutTre(dto.getTongPhutTre() + cc.getPhutDiTre());
            }

            mapLuong.put(nvId, dto);
        }

        // 2. Quy đổi ra tiền (Giả sử: 25k/giờ, phạt 1k/phút)
        BigDecimal luongMotGio = new BigDecimal("25000");
        BigDecimal phatMotPhut = new BigDecimal("1000");

        for (BangLuongDto dto : mapLuong.values()) {
            BigDecimal luongCb = luongMotGio.multiply(BigDecimal.valueOf(dto.getTongGioLam()));
            BigDecimal phat = phatMotPhut.multiply(BigDecimal.valueOf(dto.getTongPhutTre()));
            BigDecimal thucLanh = luongCb.subtract(phat);

            dto.setLuongCoBan(luongCb);
            dto.setTienPhat(phat);
            // Nếu phạt lố tiền lương thì thực lãnh = 0
            dto.setLuongThucLanh(thucLanh.compareTo(BigDecimal.ZERO) > 0 ? thucLanh : BigDecimal.ZERO);
        }

        return new ArrayList<>(mapLuong.values());
    }

    public ByteArrayInputStream exportLuongExcel(int thang, int nam) throws IOException {
        List<BangLuongDto> dsLuong = tinhLuongNhanVien(thang, nam);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bảng Lương T" + thang + "-" + nam);
            Row header = sheet.createRow(0);
            String[] columns = {"Mã NV", "Tên Nhân Viên", "Tổng Giờ Làm", "Phút Đi Trễ", "Lương Cơ Bản", "Tiền Phạt", "Thực Lãnh"};

            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            int rowIdx = 1;
            for (BangLuongDto dto : dsLuong) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getNhanVienId());
                row.createCell(1).setCellValue(dto.getTenNhanVien());
                row.createCell(2).setCellValue(Math.round(dto.getTongGioLam() * 10.0) / 10.0); // Làm tròn 1 chữ số thập phân
                row.createCell(3).setCellValue(dto.getTongPhutTre());
                row.createCell(4).setCellValue(dto.getLuongCoBan().doubleValue());
                row.createCell(5).setCellValue(dto.getTienPhat().doubleValue());
                row.createCell(6).setCellValue(dto.getLuongThucLanh().doubleValue());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
    // TASK T099: VOUCHER USAGE
    // ==========================================
    public List<VoucherUsageDto> getVoucherUsage(String maCode) {
        return donHangRepository.getVoucherUsage(maCode);
    }

// LichSuBaoCaoKiemKe
    // ==========================================

    @Autowired
private PhieuKiemKeRepository phieuKiemKeRepository;

public List<BaoCaoKiemKeDto> getBaoCaoKiemKe() {
    return phieuKiemKeRepository.findAllByOrderByNgayKiemKeDesc().stream().map(p -> new BaoCaoKiemKeDto(
            p.getId(),
            p.getSanPham().getId(),
            p.getSanPham().getTenSanPham(),
            p.getTonHeThong(),
            p.getTonThucTe(),
            p.getChenhLech(),
            p.getLyDo(),
            p.getNguoiThucHien(),
            p.getNgayKiemKe()
    )).toList();
}

}