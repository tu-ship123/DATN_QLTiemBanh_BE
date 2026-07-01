package com.poly.cake.service;

import com.poly.cake.exception.BusinessException;
import com.poly.cake.exception.ResourceNotFoundException;
import com.poly.cake.exception.ForbiddenException;

import com.poly.cake.dto.DiemVoucherDto;
import com.poly.cake.entity.*;
import com.poly.cake.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.poly.cake.entity.TrangThaiDonHang; // Import Enum
import org.springframework.dao.DataIntegrityViolationException;
@Service
@RequiredArgsConstructor
public class DiemThuongService {

    private final DiemThuongRepository diemThuongRepository;
    private final VoucherKhachHangRepository voucherRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final DonHangRepository donHangRepository;
    private final MaGiamGiaRepository maGiamGiaRepository;

    // ─── Tỷ lệ quy đổi điểm ─────────────────────────────────────────────────
    /** Cứ mỗi 10.000đ chi tiêu → 1 điểm */
    private static final int DIEM_TREN_MOI_10K = 1;
    /** Đánh giá sản phẩm → thêm 5 điểm */
    private static final int DIEM_DANH_GIA = 5;

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. LẤY TỔNG QUAN ĐIỂM CỦA KHÁCH
    // ═══════════════════════════════════════════════════════════════════════════
    public DiemVoucherDto.DiemTongQuan layDiemTongQuan(String email) {
        NguoiDung khach = timKhach(email);
        int tongDiem = diemThuongRepository.tinhTongDiem(khach);

        List<DiemVoucherDto.GiaoDichDiem> lichSu = diemThuongRepository
                .findByKhachHangOrderByNgayTaoDesc(khach)
                .stream().map(this::mapGiaoDich).collect(Collectors.toList());

        DiemVoucherDto.DiemTongQuan result = new DiemVoucherDto.DiemTongQuan();
        result.setTongDiem(tongDiem);
        result.setLichSu(lichSu);
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. LẤY DANH SÁCH MÃ GIẢM GIÁ CÓ THỂ ĐỔI BẰNG ĐIỂM
    // ═══════════════════════════════════════════════════════════════════════════
    public List<DiemVoucherDto.MaGiamGiaResponse> layDanhSachMaDoiDiem(String email) {
        NguoiDung khach = timKhach(email);
        int tongDiem = diemThuongRepository.tinhTongDiem(khach);

        return maGiamGiaRepository
                .findByDiemCanDungIsNotNullAndHoatDongTrueOrderByDiemCanDungAsc()
                .stream()
                .map(ma -> mapMaGiamGia(ma, tongDiem))
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3. LẤY DANH SÁCH VOUCHER CỦA KHÁCH
    // ═══════════════════════════════════════════════════════════════════════════
    public List<DiemVoucherDto.VoucherResponse> layVoucherCuaKhach(String email) {
        NguoiDung khach = timKhach(email);
        return voucherRepository.findByKhachHangOrderByNgayTaoDesc(khach)
                .stream().map(this::mapVoucher).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 4. ĐỔI ĐIỂM LẤY VOUCHER (dựa theo MaGiamGia)
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public DiemVoucherDto.VoucherResponse doiDiem(String email, DiemVoucherDto.DoiDiemRequest request) {
        NguoiDung khach = timKhach(email);

        // Tìm mã giảm giá hợp lệ để đổi điểm
        MaGiamGia ma = maGiamGiaRepository
                .findByMaCodeAndHoatDongTrueAndDiemCanDungIsNotNull(request.getMaGoiVoucher())
                .orElseThrow(() -> new BusinessException("Mã giảm giá không hợp lệ hoặc không thể đổi bằng điểm!"));

        int tongDiem = diemThuongRepository.tinhTongDiem(khach);
        if (tongDiem < ma.getDiemCanDung()) {
            throw new BusinessException("Không đủ điểm! Bạn có " + tongDiem + " điểm, cần " + ma.getDiemCanDung() + " điểm.");
        }

        // Kiểm tra mã còn hạn không
        if (ma.getNgayHetHan().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Mã giảm giá đã hết hạn!");
        }

        // Trừ điểm
        DiemThuong tru = DiemThuong.builder()
                .khachHang(khach)
                .diemThayDoi(-ma.getDiemCanDung())
                .loaiGiaoDich("DOI_VOUCHER")
                .moTa("Đổi " + ma.getDiemCanDung() + " điểm lấy mã: " + ma.getMaCode())
                .build();
        diemThuongRepository.save(tru);

        // Tạo voucher cá nhân cho khách từ thông tin MaGiamGia
        VoucherKhachHang voucher = VoucherKhachHang.builder()
                .khachHang(khach)
                .tenVoucher(moTaMaGiamGia(ma))
                .loaiGiam(ma.getLoaiGiamGia())
                .giaTriGiam(ma.getGiaTriGiam())
                .donHangToiThieu(ma.getDonHangToiThieu())
                .diemSuDung(ma.getDiemCanDung())
                .trangThai("CHUA_SU_DUNG")
                .ngayHetHan(LocalDateTime.now().plusDays(90))
                .build();
        voucherRepository.save(voucher);

        return mapVoucher(voucher);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 5. CỘNG ĐIỂM KHI KHÁCH XÁC NHẬN ĐÃ NHẬN HÀNG (ONLINE)
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public void congDiemXacNhanNhanHang(Long donHangId, String email) {
        NguoiDung khach = timKhach(email);
        DonHang donHang = donHangRepository.findById(donHangId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại!"));

        if (!donHang.getKhachHang().getId().equals(khach.getId())) {
            throw new ForbiddenException("Không có quyền xác nhận đơn hàng này!");
        }

        // Dùng toán tử == để so sánh Enum
        if (donHang.getTrangThai() != TrangThaiDonHang.DANG_GIAO) {
            throw new BusinessException("Đơn hàng không ở trạng thái đang giao!");
        }

        donHang.setTrangThai(TrangThaiDonHang.HOAN_THANH);
        donHangRepository.save(donHang);

        int diem = tinhDiemTheoTien(donHang.getTongTien());

        DiemThuong giaoDich = DiemThuong.builder()
                .khachHang(khach)
                .diemThayDoi(diem)
                .loaiGiaoDich("MUAT_HANG_ONLINE")
                .moTa("Xác nhận nhận đơn #" + donHangId + " – Giá trị: " + formatTien(donHang.getTongTien()))
                .donHang(donHang)
                .build();

        // Bắt lỗi Unique Constraint để chặn cộng điểm 2 lần
        try {
            diemThuongRepository.save(giaoDich);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Đơn hàng này đã được cộng điểm rồi!");
        }
    }
    // ═══════════════════════════════════════════════════════════════════════════
    // 6. CỘNG ĐIỂM KHI ĐÁNH GIÁ SẢN PHẨM
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public void congDiemDanhGia(Long donHangId, String email) {
        NguoiDung khach = timKhach(email);

        if (diemThuongRepository.existsByKhachHangAndDonHangIdAndLoaiGiaoDich(khach, donHangId, "DANH_GIA")) {
            return;
        }

        DiemThuong giaoDich = DiemThuong.builder()
                .khachHang(khach)
                .diemThayDoi(DIEM_DANH_GIA)
                .loaiGiaoDich("DANH_GIA")
                .moTa("Đánh giá sản phẩm đơn hàng #" + donHangId + " (+5 điểm)")
                .build();
        diemThuongRepository.save(giaoDich);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 7. CỘNG ĐIỂM POS (OFFLINE – nhân viên hỏi SĐT)
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public DiemVoucherDto.CongDiemPosResponse congDiemPos(DiemVoucherDto.CongDiemPosRequest request) {
        DiemVoucherDto.CongDiemPosResponse response = new DiemVoucherDto.CongDiemPosResponse();

        NguoiDung khach = nguoiDungRepository.findBySoDienThoaiAndQuyen(
                request.getSoDienThoai(), "KHACH_HANG").orElse(null);

        if (khach == null) {
            response.setTimThayKhach(false);
            response.setThongBao("Không tìm thấy tài khoản với số điện thoại: " + request.getSoDienThoai());
            return response;
        }

        DonHang donHang = donHangRepository.findById(request.getDonHangId())
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng POS không tìm thấy!"));

        // ── KHÔNG CẦN DÙNG EXISTS NỮA, THAY BẰNG TRY-CATCH KHI SAVE ──
        int diem = tinhDiemTheoTien(donHang.getTongTien());

        DiemThuong giaoDich = DiemThuong.builder()
                .khachHang(khach)
                .diemThayDoi(diem)
                .loaiGiaoDich("MUAT_HANG_POS")
                .moTa("Mua hàng tại quầy – Đơn #" + donHang.getId() + " – " + formatTien(donHang.getTongTien()))
                .donHang(donHang)
                .build();

        try {
            diemThuongRepository.save(giaoDich);
        } catch (DataIntegrityViolationException e) {
            // Nếu có exception, nghĩa là bản ghi đã tồn tại (do constraint UNIQUE ở Entity)
            response.setTimThayKhach(true);
            response.setTenKhach(khach.getHoTen());
            response.setDiemDuocCong(0);
            response.setTongDiemMoi(diemThuongRepository.tinhTongDiem(khach));
            response.setThongBao("Đơn hàng này đã được cộng điểm cho khách " + khach.getHoTen() + " rồi.");
            return response;
        }

        int tongDiemMoi = diemThuongRepository.tinhTongDiem(khach);

        response.setTimThayKhach(true);
        response.setTenKhach(khach.getHoTen());
        response.setDiemDuocCong(diem);
        response.setTongDiemMoi(tongDiemMoi);
        response.setThongBao("✅ Đã cộng " + diem + " điểm cho khách " + khach.getHoTen() + ". Tổng điểm: " + tongDiemMoi);
        return response;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER
    // ═══════════════════════════════════════════════════════════════════════════
    private NguoiDung timKhach(String email) {
        return nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản!"));
    }

    private int tinhDiemTheoTien(BigDecimal tien) {
        if (tien == null) return 0;
        return tien.divide(BigDecimal.valueOf(10_000), 0, java.math.RoundingMode.DOWN).intValue() * DIEM_TREN_MOI_10K;
    }

    private String formatTien(BigDecimal tien) {
        if (tien == null) return "0đ";
        return String.format("%,.0fđ", tien);
    }

    /** Tạo mô tả hiển thị cho khách từ MaGiamGia */
    private String moTaMaGiamGia(MaGiamGia ma) {
        if ("PHAN_TRAM".equals(ma.getLoaiGiamGia())) {
            return "Giảm " + ma.getGiaTriGiam().stripTrailingZeros().toPlainString() + "%"
                    + (ma.getDonHangToiThieu() != null && ma.getDonHangToiThieu().compareTo(BigDecimal.ZERO) > 0
                    ? " cho đơn từ " + formatTien(ma.getDonHangToiThieu()) : "");
        }
        return "Giảm " + formatTien(ma.getGiaTriGiam())
                + (ma.getDonHangToiThieu() != null && ma.getDonHangToiThieu().compareTo(BigDecimal.ZERO) > 0
                ? " cho đơn từ " + formatTien(ma.getDonHangToiThieu()) : "");
    }

    private DiemVoucherDto.GiaoDichDiem mapGiaoDich(DiemThuong d) {
        DiemVoucherDto.GiaoDichDiem dto = new DiemVoucherDto.GiaoDichDiem();
        dto.setId(d.getId());
        dto.setDiemThayDoi(d.getDiemThayDoi());
        dto.setLoaiGiaoDich(d.getLoaiGiaoDich());
        dto.setMoTa(d.getMoTa());
        dto.setDonHangId(d.getDonHang() != null ? d.getDonHang().getId() : null);
        dto.setNgayTao(d.getNgayTao());
        return dto;
    }

    private DiemVoucherDto.MaGiamGiaResponse mapMaGiamGia(MaGiamGia ma, int tongDiem) {
        DiemVoucherDto.MaGiamGiaResponse dto = new DiemVoucherDto.MaGiamGiaResponse();
        dto.setId(ma.getId());
        dto.setMaCode(ma.getMaCode());
        dto.setLoaiGiamGia(ma.getLoaiGiamGia());
        dto.setGiaTriGiam(ma.getGiaTriGiam());
        dto.setDonHangToiThieu(ma.getDonHangToiThieu());
        dto.setDiemCanDung(ma.getDiemCanDung());
        dto.setNgayHetHan(ma.getNgayHetHan());
        dto.setDuDiem(tongDiem >= ma.getDiemCanDung());
        return dto;
    }

    private DiemVoucherDto.VoucherResponse mapVoucher(VoucherKhachHang v) {
        DiemVoucherDto.VoucherResponse dto = new DiemVoucherDto.VoucherResponse();
        dto.setId(v.getId());
        dto.setTenVoucher(v.getTenVoucher());
        dto.setLoaiGiam(v.getLoaiGiam());
        dto.setGiaTriGiam(v.getGiaTriGiam());
        dto.setDonHangToiThieu(v.getDonHangToiThieu());
        dto.setDiemSuDung(v.getDiemSuDung());
        dto.setTrangThai(v.getTrangThai());
        dto.setNgayHetHan(v.getNgayHetHan());
        dto.setNgayTao(v.getNgayTao());
        dto.setConHieuLuc("CHUA_SU_DUNG".equals(v.getTrangThai()) && v.getNgayHetHan().isAfter(LocalDateTime.now()));
        return dto;
    }
}