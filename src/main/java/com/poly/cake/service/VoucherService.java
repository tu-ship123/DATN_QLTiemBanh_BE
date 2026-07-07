package com.poly.cake.service;

import com.poly.cake.dto.VoucherValidateDto;
import com.poly.cake.entity.MaGiamGia;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.VoucherKhachHang;
import com.poly.cake.exception.BusinessException;
import com.poly.cake.repository.MaGiamGiaRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.VoucherKhachHangRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * T070 – Kiểm tra ĐẦY ĐỦ điều kiện của mã giảm giá / voucher cá nhân, tách
 * riêng khỏi GioHangService và OrderService để dùng chung được cho cả khách
 * vãng lai (guest, không có giỏ hàng persist) lẫn khách đã đăng nhập, mà
 * KHÔNG cần phải thao tác/đụng vào giỏ hàng.
 */
@Service
@RequiredArgsConstructor
public class VoucherService {

    private final MaGiamGiaRepository maGiamGiaRepository;
    private final VoucherKhachHangRepository voucherKhachHangRepository;
    private final NguoiDungRepository nguoiDungRepository;

    /**
     * @param emailNguoiDung email khách đã đăng nhập, NULL nếu là khách vãng lai (guest)
     */
    public VoucherValidateDto.Response validate(VoucherValidateDto.Request request, String emailNguoiDung) {
        boolean coMaCode = request.getMaCode() != null && !request.getMaCode().isBlank();
        boolean coVoucherCaNhan = request.getVoucherKhachHangId() != null;

        if (coMaCode == coVoucherCaNhan) {
            // Cả 2 cùng trống hoặc cùng có -> yêu cầu không rõ ràng
            throw new BusinessException("Vui lòng gửi ĐÚNG 1 trong 2: mã giảm giá (maCode) hoặc voucherKhachHangId!");
        }

        BigDecimal tongTienHang = request.getTongTienHang();

        if (coMaCode) {
            return validateMaGiamGia(request.getMaCode().trim(), tongTienHang);
        }

        // ── Voucher cá nhân: bắt buộc phải đăng nhập vì voucher gắn với tài khoản ──
        if (emailNguoiDung == null || emailNguoiDung.isBlank()) {
            throw new BusinessException("Voucher cá nhân chỉ áp dụng được khi bạn đã đăng nhập!");
        }
        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailNguoiDung)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin tài khoản!"));

        return validateVoucherCaNhan(request.getVoucherKhachHangId(), khachHang, tongTienHang);
    }

    // ─── MÃ GIẢM GIÁ (public code) ────────────────────────────────────────────
    private VoucherValidateDto.Response validateMaGiamGia(String maCode, BigDecimal tongTienHang) {
        VoucherValidateDto.Response res = new VoucherValidateDto.Response();
        res.setLoaiUuDai("MA_GIAM_GIA");
        res.setMaCode(maCode);

        MaGiamGia ma = maGiamGiaRepository.findByMaCode(maCode).orElse(null);
        if (ma == null) {
            return khongHopLe(res, "Mã giảm giá \"" + maCode + "\" không tồn tại!");
        }

        res.setLoaiGiamGia(ma.getLoaiGiamGia());
        res.setGiaTriGiam(ma.getGiaTriGiam());
        res.setDonHangToiThieu(ma.getDonHangToiThieu());

        // Điều kiện 1: đang hoạt động
        if (!Boolean.TRUE.equals(ma.getHoatDong())) {
            return khongHopLe(res, "Mã giảm giá này hiện không còn hoạt động!");
        }
        // Điều kiện 2: chưa hết hạn
        if (ma.getNgayHetHan() != null && ma.getNgayHetHan().isBefore(LocalDateTime.now())) {
            return khongHopLe(res, "Mã giảm giá này đã hết hạn!");
        }
        // Điều kiện 3: còn lượt sử dụng
        if (ma.getSoLuotToiDa() != null && ma.getSoLuotDaDung() != null
                && ma.getSoLuotDaDung() >= ma.getSoLuotToiDa()) {
            return khongHopLe(res, "Mã giảm giá này đã hết lượt sử dụng!");
        }
        // Điều kiện 4: đơn hàng tối thiểu
        if (ma.getDonHangToiThieu() != null && tongTienHang.compareTo(ma.getDonHangToiThieu()) < 0) {
            return khongHopLe(res, "Đơn hàng chưa đạt giá trị tối thiểu "
                    + ma.getDonHangToiThieu() + " để áp dụng mã này!");
        }

        BigDecimal soTienGiam = tinhSoTienGiam(ma.getLoaiGiamGia(), ma.getGiaTriGiam(), tongTienHang);
        return hopLe(res, soTienGiam, tongTienHang);
    }

    // ─── VOUCHER CÁ NHÂN (đổi bằng điểm / được tặng) ───────────────────────────
    private VoucherValidateDto.Response validateVoucherCaNhan(Long voucherId, NguoiDung khachHang, BigDecimal tongTienHang) {
        VoucherValidateDto.Response res = new VoucherValidateDto.Response();
        res.setLoaiUuDai("VOUCHER_CA_NHAN");

        VoucherKhachHang voucher = voucherKhachHangRepository
                .findByIdAndKhachHang(voucherId, khachHang).orElse(null);
        if (voucher == null) {
            return khongHopLe(res, "Không tìm thấy voucher này trong tài khoản của bạn!");
        }

        res.setTenVoucher(voucher.getTenVoucher());
        res.setLoaiGiamGia(voucher.getLoaiGiam());
        res.setGiaTriGiam(voucher.getGiaTriGiam());
        res.setDonHangToiThieu(voucher.getDonHangToiThieu());

        // Điều kiện 1: chưa sử dụng
        if (!"CHUA_SU_DUNG".equals(voucher.getTrangThai())) {
            return khongHopLe(res, "Voucher này đã được sử dụng hoặc không còn hiệu lực!");
        }
        // Điều kiện 2: chưa hết hạn
        if (voucher.getNgayHetHan() != null && voucher.getNgayHetHan().isBefore(LocalDateTime.now())) {
            return khongHopLe(res, "Voucher này đã hết hạn!");
        }
        // Điều kiện 3: đơn hàng tối thiểu
        if (voucher.getDonHangToiThieu() != null && tongTienHang.compareTo(voucher.getDonHangToiThieu()) < 0) {
            return khongHopLe(res, "Đơn hàng chưa đạt giá trị tối thiểu "
                    + voucher.getDonHangToiThieu() + " để áp dụng voucher này!");
        }

        BigDecimal soTienGiam = tinhSoTienGiam(voucher.getLoaiGiam(), voucher.getGiaTriGiam(), tongTienHang);
        return hopLe(res, soTienGiam, tongTienHang);
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────────
    private BigDecimal tinhSoTienGiam(String loaiGiam, BigDecimal giaTriGiam, BigDecimal tongTienHang) {
        BigDecimal soTienGiam;
        if ("PHAN_TRAM".equals(loaiGiam)) {
            soTienGiam = tongTienHang.multiply(giaTriGiam).divide(BigDecimal.valueOf(100));
        } else {
            soTienGiam = giaTriGiam;
        }
        // Không cho số tiền giảm vượt quá tổng tiền hàng
        return soTienGiam.min(tongTienHang);
    }

    private VoucherValidateDto.Response khongHopLe(VoucherValidateDto.Response res, String message) {
        res.setHopLe(false);
        res.setSoTienGiam(BigDecimal.ZERO);
        res.setMessage(message);
        return res;
    }

    private VoucherValidateDto.Response hopLe(VoucherValidateDto.Response res, BigDecimal soTienGiam, BigDecimal tongTienHang) {
        res.setHopLe(true);
        res.setSoTienGiam(soTienGiam);
        res.setTongTienSauGiam(tongTienHang.subtract(soTienGiam));
        res.setMessage("Áp dụng thành công! Bạn được giảm " + soTienGiam + "đ.");
        return res;
    }
}
