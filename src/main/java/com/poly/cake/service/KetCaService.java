package com.poly.cake.service;

import com.poly.cake.exception.NgoaiLeNghiepVu;
import com.poly.cake.exception.NgoaiLeKhongTimThayTaiNguyen;
import com.poly.cake.exception.NgoaiLeCamTruyCap;

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
import java.util.stream.Collectors;

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
                .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen("Không tìm thấy người dùng: " + email));
    }

    @Transactional
    public ChamCongResponse ketCa(KetCaRequest request) {
        NguoiDung nhanVien = getNhanVienHienTai();

        String loai = request.getLoaiBaoCao() == null
                ? null : request.getLoaiBaoCao().toUpperCase().trim();
        if (loai == null || !LOAI_HOP_LE.contains(loai)) {
            throw new NgoaiLeNghiepVu(
                    "loaiBaoCao không hợp lệ. Chỉ chấp nhận: X_REPORT hoặc Z_REPORT.");
        }

        PhanCa phanCa = phanCaRepository
                .findByIdAndNhanVienId(request.getPhanCaId(), nhanVien.getId())
                .orElseThrow(() -> new NgoaiLeCamTruyCap(
                        "Không tìm thấy phân ca #" + request.getPhanCaId()
                                + " hoặc bạn không có quyền truy cập."));

        switch (phanCa.getTrangThai()) {
            case "DA_HUY":
                throw new NgoaiLeNghiepVu("Ca này đã bị hủy, không thể kết ca.");
            case "DA_KET_CA":
                throw new NgoaiLeNghiepVu(
                        "Ca này đã kết ca chính thức (Z-Report) rồi, không thể thực hiện lại.");
            case "DA_LAP":
                throw new NgoaiLeNghiepVu(
                        "Bạn chưa check-in ca này. Hãy check-in trước khi kết ca.");
            default:
                break;
        }

        ChamCong chamCong = chamCongRepository.findByPhanCa(phanCa)
                .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen(
                        "Không tìm thấy bản ghi chấm công cho ca này."));

        if (Z_REPORT.equals(chamCong.getLoaiBaoCao())) {
            throw new NgoaiLeNghiepVu("Ca này đã có Z-Report rồi.");
        }

        LocalDateTime tuThoiDiem = chamCong.getGioVao();

        // FIX: nếu nhân viên ĐÃ check-out (chamCong.getGioRa() đã có giá trị),
        // phải chốt mốc kết thúc = giờ check-out thật, KHÔNG được dùng
        // LocalDateTime.now(). Trước đây luôn dùng now() nên nếu nhân viên
        // check-out lúc 10:04 rồi vài tiếng sau (VD 15:50) mới vào bấm
        // X-Report, hệ thống sẽ tính doanh thu trong khoảng [gioVao, 15:50]
        // thay vì đúng ra phải là [gioVao, gioRa] — dễ gộp nhầm doanh thu của
        // ca/nhân viên khác phát sinh sau khi ca này đã kết thúc.
        LocalDateTime denThoiDiem = chamCong.getGioRa() != null
                ? chamCong.getGioRa()
                : LocalDateTime.now();

        if (tuThoiDiem == null) {
            throw new NgoaiLeNghiepVu("Dữ liệu chấm công không hợp lệ (giờ vào trống).");
        }

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

        chamCong.setThoiDiemKetCa(denThoiDiem);
        chamCong.setLoaiBaoCao(loai);
        chamCong.setTongSoDon(tongSoDon != null ? tongSoDon.intValue() : 0);
        chamCong.setDoanhThuTienMat(dtTienMat);
        chamCong.setDoanhThuSepay(dtSepay);
        chamCong.setDoanhThuKhac(dtKhac);
        chamCong.setTongDoanhThu(tongDoanhThu);
        chamCong.setGhiChuKetCa(request.getGhiChu());

        if (Z_REPORT.equals(loai)) {
            phanCa.setTrangThai("DA_KET_CA");
            phanCaRepository.save(phanCa);

            if (chamCong.getGioRa() == null) {
                chamCong.setGioRa(denThoiDiem);
            }
        }

        chamCong = chamCongRepository.save(chamCong);
        return mapToResponse(chamCong);
    }

    public ChamCongResponse getBaoCaoByPhanCa(Long phanCaId) {
        NguoiDung nhanVien = getNhanVienHienTai();

        PhanCa phanCa = phanCaRepository
                .findByIdAndNhanVienId(phanCaId, nhanVien.getId())
                .orElseThrow(() -> new NgoaiLeCamTruyCap(
                        "Không tìm thấy phân ca #" + phanCaId + " hoặc không có quyền xem."));

        ChamCong chamCong = chamCongRepository.findByPhanCa(phanCa)
                .orElseThrow(() -> new NgoaiLeNghiepVu(
                        "Chưa có bản ghi chấm công cho ca này (chưa check-in)."));

        return mapToResponse(chamCong);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADMIN: Xem báo cáo theo ngày (ĐÃ SỬA LỖI TRÀN RAM)
    // ═══════════════════════════════════════════════════════════════════════════
    public List<ChamCongResponse> getAdminBaoCaoTheoNgay(java.time.LocalDate ngay) {
        java.time.LocalDate target = ngay != null ? ngay : java.time.LocalDate.now();

        // Gọi thẳng hàm Repository đã được tối ưu thay vì dùng findAll()
        return chamCongRepository.findByNgayLamViecAndCoLoaiBaoCao(target).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

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