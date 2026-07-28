package com.poly.cake.service;

import com.poly.cake.dto.*;
import com.poly.cake.repository.DonHangRepository;
import com.poly.cake.repository.PhieuKiemKeRepository;
import com.poly.cake.repository.ThanhToanRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    // BỘ MÀU & STYLE DÙNG CHUNG CHO FILE EXCEL
    // (theo đúng tông thương hiệu Polycake: amber/cam đậm + kem, xem Login.vue)
    // ==========================================
    private static final byte[] MAU_AMBER     = new byte[]{(byte) 212, (byte) 133, (byte) 10};  // #D4850A
    private static final byte[] MAU_AMBER_DAM = new byte[]{(byte) 61,  (byte) 32,  (byte) 0};    // #3D2000 (brown-dark)
    private static final byte[] MAU_KEM       = new byte[]{(byte) 255, (byte) 253, (byte) 208};  // #FFFDD0
    private static final byte[] MAU_KEM_NHAT  = new byte[]{(byte) 253, (byte) 243, (byte) 220};  // #FDF3DC
    private static final byte[] MAU_XANH_LA   = new byte[]{(byte) 58,  (byte) 138, (byte) 32};   // #3A8A20 (thực lãnh)
    private static final byte[] MAU_DO        = new byte[]{(byte) 224, (byte) 70,  (byte) 60};   // #E0463C (tiền phạt)

    private static final String DINH_DANG_TIEN = "#,##0 \"₫\"";

    // Ghi khối tiêu đề báo cáo (tên báo cáo + thời gian xuất) và merge ngang qua các cột dữ liệu
    private void ghiTieuDeBaoCao(XSSFWorkbook wb, Sheet sheet, String tieuDe, String ngayXuat, int cotCuoiChiSo0) {
        Row rowTitle = sheet.createRow(0);
        rowTitle.setHeightInPoints(28);
        Cell cTitle = rowTitle.createCell(0);
        cTitle.setCellValue("POLYCAKE — " + tieuDe);
        cTitle.setCellStyle(styleTieuDe(wb));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, cotCuoiChiSo0));

        Row rowSub = sheet.createRow(1);
        rowSub.setHeightInPoints(16);
        Cell cSub = rowSub.createCell(0);
        cSub.setCellValue("Xuất báo cáo lúc " + ngayXuat);
        cSub.setCellStyle(styleGhiChu(wb));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, cotCuoiChiSo0));
        // Dòng 2 để trống làm khoảng cách trước khi vào bảng header (dòng 3)
    }

    private XSSFCellStyle styleTieuDe(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 16);
        f.setColor(new XSSFColor(MAU_AMBER_DAM, null));
        st.setFont(f);
        st.setAlignment(HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        return st;
    }

    private XSSFCellStyle styleGhiChu(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setItalic(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(new XSSFColor(MAU_AMBER_DAM, null));
        st.setFont(f);
        st.setAlignment(HorizontalAlignment.CENTER);
        return st;
    }

    private void ganDuongVien(XSSFCellStyle st) {
        st.setBorderTop(BorderStyle.THIN);
        st.setBorderBottom(BorderStyle.THIN);
        st.setBorderLeft(BorderStyle.THIN);
        st.setBorderRight(BorderStyle.THIN);
    }

    private XSSFCellStyle styleHeader(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        f.setFontHeightInPoints((short) 11);
        st.setFont(f);
        st.setFillForegroundColor(new XSSFColor(MAU_AMBER, null));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        st.setWrapText(true);
        ganDuongVien(st);
        return st;
    }

    // Style cho ô dữ liệu thường, có tô sọc xen kẽ (banded rows) để dễ đọc
    private XSSFCellStyle styleDuLieu(XSSFWorkbook wb, boolean dongXen, boolean canPhai) {
        XSSFCellStyle st = wb.createCellStyle();
        if (dongXen) {
            st.setFillForegroundColor(new XSSFColor(MAU_KEM_NHAT, null));
            st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        st.setAlignment(canPhai ? HorizontalAlignment.RIGHT : HorizontalAlignment.LEFT);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        ganDuongVien(st);
        return st;
    }

    // Style cho ô tiền tệ: có định dạng "#,##0 ₫", căn phải, tô sọc xen kẽ
    private XSSFCellStyle styleTien(XSSFWorkbook wb, boolean dongXen) {
        XSSFCellStyle st = styleDuLieu(wb, dongXen, true);
        DataFormat fmt = wb.createDataFormat();
        st.setDataFormat(fmt.getFormat(DINH_DANG_TIEN));
        return st;
    }

    // Style cho ô tiền tệ có tô màu chữ riêng (VD: tiền phạt màu đỏ, thực lãnh màu xanh)
    private XSSFCellStyle styleTienMauChu(XSSFWorkbook wb, boolean dongXen, byte[] mauChu, boolean inDam) {
        XSSFCellStyle st = styleTien(wb, dongXen);
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(mauChu, null));
        f.setBold(inDam);
        st.setFont(f);
        return st;
    }

    // Style cho dòng "TỔNG CỘNG" ở cuối bảng: viền đôi phía trên, tô nền kem, chữ đậm màu nâu
    private XSSFCellStyle styleTongCong(XSSFWorkbook wb, boolean laTien) {
        XSSFCellStyle st = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        f.setColor(new XSSFColor(MAU_AMBER_DAM, null));
        st.setFont(f);
        st.setFillForegroundColor(new XSSFColor(MAU_KEM, null));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        ganDuongVien(st);
        st.setBorderTop(BorderStyle.DOUBLE);
        st.setAlignment(laTien ? HorizontalAlignment.RIGHT : HorizontalAlignment.LEFT);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        if (laTien) {
            DataFormat fmt = wb.createDataFormat();
            st.setDataFormat(fmt.getFormat(DINH_DANG_TIEN));
        }
        return st;
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

    // Code Generate file Excel — có tiêu đề, header màu thương hiệu, banded rows,
    // định dạng tiền tệ, dòng tổng cộng và freeze pane thay vì bảng trắng đơn điệu
    public ByteArrayInputStream exportReportToExcel() throws IOException {
        List<DoanhThuKenhDto> doanhThuList = getDoanhThuKenh();
        List<TopSanPhamDto> topSanPhamList = getTopSanPham();

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String ngayXuat = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy"));

            // --- SHEET 1: DOANH THU KÊNH ---
            Sheet sheet1 = workbook.createSheet("Doanh Thu Kênh");
            ghiTieuDeBaoCao(workbook, sheet1, "BÁO CÁO DOANH THU THEO KÊNH BÁN", ngayXuat, 1);

            int hRow1 = 3; // chừa dòng 2 (index 1) làm ghi chú, dòng 3 (index 2) trống
            Row header1 = sheet1.createRow(hRow1);
            header1.setHeightInPoints(22);
            String[] cols1 = {"Kênh Bán (Nguồn Đơn)", "Tổng Doanh Thu"};
            for (int i = 0; i < cols1.length; i++) {
                Cell c = header1.createCell(i);
                c.setCellValue(cols1[i]);
                c.setCellStyle(styleHeader(workbook));
            }

            int rowIdx1 = hRow1 + 1;
            boolean dong = false;
            BigDecimal tongDoanhThu = BigDecimal.ZERO;
            for (DoanhThuKenhDto dt : doanhThuList) {
                Row row = sheet1.createRow(rowIdx1++);
                row.setHeightInPoints(19);

                Cell c0 = row.createCell(0);
                c0.setCellValue(dt.getNguonDon());
                c0.setCellStyle(styleDuLieu(workbook, dong, false));

                Cell c1 = row.createCell(1);
                c1.setCellValue(dt.getTongDoanhThu().doubleValue());
                c1.setCellStyle(styleTien(workbook, dong));

                tongDoanhThu = tongDoanhThu.add(dt.getTongDoanhThu());
                dong = !dong;
            }

            Row rowTong1 = sheet1.createRow(rowIdx1);
            rowTong1.setHeightInPoints(20);
            Cell tc0 = rowTong1.createCell(0);
            tc0.setCellValue("TỔNG CỘNG");
            tc0.setCellStyle(styleTongCong(workbook, false));
            Cell tc1 = rowTong1.createCell(1);
            tc1.setCellValue(tongDoanhThu.doubleValue());
            tc1.setCellStyle(styleTongCong(workbook, true));

            sheet1.setColumnWidth(0, 34 * 256);
            sheet1.setColumnWidth(1, 24 * 256);
            sheet1.createFreezePane(0, hRow1 + 1);

            // --- SHEET 2: TOP SẢN PHẨM ---
            Sheet sheet2 = workbook.createSheet("Top Sản Phẩm Bán Chạy");
            ghiTieuDeBaoCao(workbook, sheet2, "TOP 10 SẢN PHẨM BÁN CHẠY NHẤT", ngayXuat, 2);

            int hRow2 = 3;
            Row header2 = sheet2.createRow(hRow2);
            header2.setHeightInPoints(22);
            String[] cols2 = {"Hạng", "Tên Sản Phẩm", "Số Lượng Bán Ra"};
            for (int i = 0; i < cols2.length; i++) {
                Cell c = header2.createCell(i);
                c.setCellValue(cols2[i]);
                c.setCellStyle(styleHeader(workbook));
            }

            int rowIdx2 = hRow2 + 1;
            int hang = 1;
            dong = false;
            for (TopSanPhamDto sp : topSanPhamList) {
                Row row = sheet2.createRow(rowIdx2++);
                row.setHeightInPoints(19);

                Cell c0 = row.createCell(0);
                c0.setCellValue(hang++);
                c0.setCellStyle(styleDuLieu(workbook, dong, true));

                Cell c1 = row.createCell(1);
                c1.setCellValue(sp.getTenSanPham());
                c1.setCellStyle(styleDuLieu(workbook, dong, false));

                Cell c2 = row.createCell(2);
                c2.setCellValue(sp.getTongSoLuongBan());
                c2.setCellStyle(styleDuLieu(workbook, dong, true));

                dong = !dong;
            }

            sheet2.setColumnWidth(0, 8 * 256);
            sheet2.setColumnWidth(1, 40 * 256);
            sheet2.setColumnWidth(2, 22 * 256);
            sheet2.createFreezePane(0, hRow2 + 1);

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

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String ngayXuat = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy"));
            Sheet sheet = workbook.createSheet("Bảng Lương T" + thang + "-" + nam);
            ghiTieuDeBaoCao(workbook, sheet, "BẢNG LƯƠNG NHÂN VIÊN THÁNG " + thang + "/" + nam, ngayXuat, 6);

            int hRow = 3;
            Row header = sheet.createRow(hRow);
            header.setHeightInPoints(24);
            String[] columns = {"Mã NV", "Tên Nhân Viên", "Tổng Giờ Làm", "Phút Đi Trễ", "Lương Cơ Bản", "Tiền Phạt", "Thực Lãnh"};
            for (int i = 0; i < columns.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(columns[i]);
                c.setCellStyle(styleHeader(workbook));
            }

            int rowIdx = hRow + 1;
            boolean dong = false;
            BigDecimal tongCoBan = BigDecimal.ZERO;
            BigDecimal tongPhat = BigDecimal.ZERO;
            BigDecimal tongThucLanh = BigDecimal.ZERO;

            for (BangLuongDto dto : dsLuong) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(19);

                Cell c0 = row.createCell(0);
                c0.setCellValue(dto.getNhanVienId());
                c0.setCellStyle(styleDuLieu(workbook, dong, true));

                Cell c1 = row.createCell(1);
                c1.setCellValue(dto.getTenNhanVien());
                c1.setCellStyle(styleDuLieu(workbook, dong, false));

                Cell c2 = row.createCell(2);
                c2.setCellValue(Math.round(dto.getTongGioLam() * 10.0) / 10.0); // Làm tròn 1 chữ số thập phân
                c2.setCellStyle(styleDuLieu(workbook, dong, true));

                // Phút đi trễ > 0 -> tô chữ đỏ, đậm để nổi bật cảnh báo
                Cell c3 = row.createCell(3);
                c3.setCellValue(dto.getTongPhutTre());
                if (dto.getTongPhutTre() > 0) {
                    XSSFCellStyle stDoTre = styleDuLieu(workbook, dong, true);
                    XSSFFont fDoTre = workbook.createFont();
                    fDoTre.setColor(new XSSFColor(MAU_DO, null));
                    fDoTre.setBold(true);
                    stDoTre.setFont(fDoTre);
                    c3.setCellStyle(stDoTre);
                } else {
                    c3.setCellStyle(styleDuLieu(workbook, dong, true));
                }

                Cell c4 = row.createCell(4);
                c4.setCellValue(dto.getLuongCoBan().doubleValue());
                c4.setCellStyle(styleTien(workbook, dong));

                Cell c5 = row.createCell(5);
                c5.setCellValue(dto.getTienPhat().doubleValue());
                c5.setCellStyle(styleTienMauChu(workbook, dong, MAU_DO, false));

                Cell c6 = row.createCell(6);
                c6.setCellValue(dto.getLuongThucLanh().doubleValue());
                c6.setCellStyle(styleTienMauChu(workbook, dong, MAU_XANH_LA, true));

                tongCoBan = tongCoBan.add(dto.getLuongCoBan());
                tongPhat = tongPhat.add(dto.getTienPhat());
                tongThucLanh = tongThucLanh.add(dto.getLuongThucLanh());
                dong = !dong;
            }

            // --- Dòng tổng cộng ---
            Row rowTong = sheet.createRow(rowIdx);
            rowTong.setHeightInPoints(20);
            Cell t0 = rowTong.createCell(0);
            t0.setCellValue("TỔNG CỘNG");
            t0.setCellStyle(styleTongCong(workbook, false));
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 3));
            for (int i = 1; i <= 3; i++) {
                Cell filler = rowTong.createCell(i);
                filler.setCellStyle(styleTongCong(workbook, false));
            }
            Cell t4 = rowTong.createCell(4);
            t4.setCellValue(tongCoBan.doubleValue());
            t4.setCellStyle(styleTongCong(workbook, true));
            Cell t5 = rowTong.createCell(5);
            t5.setCellValue(tongPhat.doubleValue());
            t5.setCellStyle(styleTongCong(workbook, true));
            Cell t6 = rowTong.createCell(6);
            t6.setCellValue(tongThucLanh.doubleValue());
            t6.setCellStyle(styleTongCong(workbook, true));

            int[] doRong = {10, 26, 14, 14, 16, 14, 16};
            for (int i = 0; i < doRong.length; i++) {
                sheet.setColumnWidth(i, doRong[i] * 256);
            }
            sheet.createFreezePane(0, hRow + 1);

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