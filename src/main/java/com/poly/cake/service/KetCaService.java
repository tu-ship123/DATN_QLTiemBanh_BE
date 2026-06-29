package com.poly.cake.service;

import com.poly.cake.dto.ChamCongResponse;
import com.poly.cake.dto.KetCaRequest;
import com.poly.cake.entity.ChamCong;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.PhanCa;
import com.poly.cake.repository.ChamCongRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.PhanCaRepository;
import com.poly.cake.repository.ThanhToanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * T062 – Kết ca: tổng hợp doanh thu tiền mặt / SePay trong ca
 * và lưu kết quả vào bản ghi ChamCong hiện có (không tạo bảng mới).
 *
 * Luồng:
 *   Nhân viên đã check-in (ChamCong.gioVao ≠ null)
 *   → nhấn "Kết ca" (X_REPORT hoặc Z_REPORT)
 *   → query ThanhToan theo [gioVao, now) để tổng hợp doanh thu
 *   → ghi kết quả vào ChamCong
 *   → nếu Z_REPORT: PhanCa.trangThai = DA_KET_CA + ghi gioRa
 */
@Service
@RequiredArgsConstructor
public class KetCaService {

    private static final String TIEN_MAT     = "TIEN_MAT";
    private static final String CHUYEN_KHOAN = "CHUYEN_KHOAN"; // SePay
    private static final String X_REPORT     = "X_REPORT";
    private static final String Z_REPORT     = "Z_REPORT";
    private static final Set<String> LOAI_HOP_LE = Set.of(X_REPORT, Z_REPORT);

    private final PhanCaRepository    phanCaRepository;
    private final ChamCongRepository  chamCongRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final NguoiDungRepository nguoiDungRepository;

    private NguoiDung getNhanVienHienTai() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NHÂN VIÊN: Kết ca (POST /api/v1/staff/ket-ca)
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public ChamCongResponse ketCa(KetCaRequest request) {
        NguoiDung nhanVien = getNhanVienHienTai();

        // 1. Validate loại báo cáo
        String loai = request.getLoaiBaoCao() == null
                ? null : request.getLoaiBaoCao().toUpperCase().trim();
        if (loai == null || !LOAI_HOP_LE.contains(loai)) {
            throw new RuntimeException(
                    "loaiBaoCao không hợp lệ. Chỉ chấp nhận: X_REPORT hoặc Z_REPORT.");
        }

        // 2. Tìm phân ca và kiểm tra quyền
        PhanCa phanCa = phanCaRepository
                .findByIdAndNhanVienId(request.getPhanCaId(), nhanVien.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy phân ca #" + request.getPhanCaId()
                        + " hoặc bạn không có quyền truy cập."));

        // 3. Validate trạng thái phân ca
        switch (phanCa.getTrangThai()) {
            case "DA_HUY":
                throw new RuntimeException("Ca này đã bị hủy, không thể kết ca.");
            case "DA_KET_CA":
                throw new RuntimeException(
                        "Ca này đã kết ca chính thức (Z-Report) rồi, không thể thực hiện lại.");
            case "DA_LAP":
                throw new RuntimeException(
                        "Bạn chưa check-in ca này. Hãy check-in trước khi kết ca.");
            default:
                // XAC_NHAN → OK, tiếp tục
                break;
        }

        // 4. Lấy bản ghi ChamCong (đã tồn tại do check-in)
        ChamCong chamCong = chamCongRepository.findByPhanCa(phanCa)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy bản ghi chấm công cho ca này."));

        // 5. Chỉ cho phép X_REPORT nếu chưa có Z_REPORT
        //    (ChamCong.loaiBaoCao = Z_REPORT → đã kết ca rồi — phòng thủ thêm)
        if (Z_REPORT.equals(chamCong.getLoaiBaoCao())) {
            throw new RuntimeException("Ca này đã có Z-Report rồi.");
        }

        LocalDateTime tuThoiDiem  = chamCong.getGioVao();
        LocalDateTime denThoiDiem = LocalDateTime.now();

        if (tuThoiDiem == null) {
            throw new RuntimeException("Dữ liệu chấm công không hợp lệ (giờ vào trống).");
        }

        // 6. Tổng hợp doanh thu từ ThanhToan trong [gioVao, now)
        List<Object[]> rows = thanhToanRepository
                .sumDoanhThuTheoHinhThuc(tuThoiDiem, denThoiDiem);

        BigDecimal dtTienMat = BigDecimal.ZERO;
        BigDecimal dtSepay   = BigDecimal.ZERO;
        BigDecimal dtKhac    = BigDecimal.ZERO;

        for (Object[] row : rows) {
            String     hinhThuc = (String) row[0];
            BigDecimal tong     = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;

            if (TIEN_MAT.equalsIgnoreCase(hinhThuc))     dtTienMat = dtTienMat.add(tong);
            else if (CHUYEN_KHOAN.equalsIgnoreCase(hinhThuc)) dtSepay = dtSepay.add(tong);
            else                                              dtKhac  = dtKhac.add(tong);
        }

        BigDecimal tongDoanhThu = dtTienMat.add(dtSepay).add(dtKhac);
        Long tongSoDon = thanhToanRepository.countDonThanhCong(tuThoiDiem, denThoiDiem);

        // 7. Ghi kết quả vào ChamCong
        chamCong.setThoiDiemKetCa(denThoiDiem);
        chamCong.setLoaiBaoCao(loai);
        chamCong.setTongSoDon(tongSoDon != null ? tongSoDon.intValue() : 0);
        chamCong.setDoanhThuTienMat(dtTienMat);
        chamCong.setDoanhThuSepay(dtSepay);
        chamCong.setDoanhThuKhac(dtKhac);
        chamCong.setTongDoanhThu(tongDoanhThu);
        chamCong.setGhiChuKetCa(request.getGhiChu());

        // 8. Z_REPORT: đóng ca chính thức
        if (Z_REPORT.equals(loai)) {
            phanCa.setTrangThai("DA_KET_CA");
            phanCaRepository.save(phanCa);

            // Tự động ghi giờ ra nếu nhân viên chưa checkout
            if (chamCong.getGioRa() == null) {
                chamCong.setGioRa(denThoiDiem);
            }
        }

        chamCong = chamCongRepository.save(chamCong);
        return mapToResponse(chamCong);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NHÂN VIÊN: Xem báo cáo ca hiện tại (GET /api/v1/staff/ket-ca/{phanCaId})
    // ═══════════════════════════════════════════════════════════════════════════

    public ChamCongResponse getBaoCaoByPhanCa(Long phanCaId) {
        NguoiDung nhanVien = getNhanVienHienTai();

        PhanCa phanCa = phanCaRepository
                .findByIdAndNhanVienId(phanCaId, nhanVien.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy phân ca #" + phanCaId + " hoặc không có quyền xem."));

        ChamCong chamCong = chamCongRepository.findByPhanCa(phanCa)
                .orElseThrow(() -> new RuntimeException(
                        "Chưa có bản ghi chấm công cho ca này (chưa check-in)."));

        return mapToResponse(chamCong);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADMIN: Xem báo cáo theo ngày (trả về toàn bộ ChamCong đã kết ca)
    // GET /api/v1/admin/ket-ca?ngay=2025-06-28
    // ═══════════════════════════════════════════════════════════════════════════

    public List<ChamCongResponse> getAdminBaoCaoTheoNgay(java.time.LocalDate ngay) {
        java.time.LocalDate target = ngay != null ? ngay : java.time.LocalDate.now();
        // Lấy tất cả PhanCa trong ngày đó có loaiBaoCao != null (đã kết ca X hoặc Z)
        return chamCongRepository.findAll().stream()
                .filter(cc -> cc.getPhanCa().getNgayLamViec().equals(target)
                           && cc.getLoaiBaoCao() != null)
                .map(this::mapToResponse)
                .toList();
    }

    // ── HELPER ────────────────────────────────────────────────────────────────

    private ChamCongResponse mapToResponse(ChamCong cc) {
        String label = null;
        if (Z_REPORT.equals(cc.getLoaiBaoCao()))      label = "Z-Report (Kết ca chính thức)";
        else if (X_REPORT.equals(cc.getLoaiBaoCao())) label = "X-Report (Báo cáo giữa ca)";

        return ChamCongResponse.builder()
                .id(cc.getId())
                .phanCaId(cc.getPhanCa().getId())
                .tenCa(cc.getPhanCa().getCaLamViec().getTenCa())
                .ngayLamViec(cc.getPhanCa().getNgayLamViec().toString())
                .gioVao(cc.getGioVao())
                .gioRa(cc.getGioRa())
                .phutDiTre(cc.getPhutDiTre())
                .trangThai(cc.getTrangThai())
                // T062
                .thoiDiemKetCa(cc.getThoiDiemKetCa())
                .loaiBaoCao(cc.getLoaiBaoCao())
                .loaiBaoCaoLabel(label)
                .tongSoDon(cc.getTongSoDon())
                .doanhThuTienMat(cc.getDoanhThuTienMat())
                .doanhThuSepay(cc.getDoanhThuSepay())
                .doanhThuKhac(cc.getDoanhThuKhac())
                .tongDoanhThu(cc.getTongDoanhThu())
                .ghiChuKetCa(cc.getGhiChuKetCa())
                .build();
    }
}