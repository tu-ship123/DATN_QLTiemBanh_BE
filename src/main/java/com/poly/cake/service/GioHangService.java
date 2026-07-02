package com.poly.cake.service;

import com.poly.cake.exception.BusinessException;
import com.poly.cake.exception.ResourceNotFoundException;

import com.poly.cake.dto.GioHangDto;
import com.poly.cake.entity.ChiTietGioHang;
import com.poly.cake.entity.GioHang;
import com.poly.cake.entity.MaGiamGia;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.SanPham;
import com.poly.cake.entity.VoucherKhachHang;
import com.poly.cake.repository.GioHangRepository;
import com.poly.cake.repository.MaGiamGiaRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.SanPhamRepository;
import com.poly.cake.repository.VoucherKhachHangRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GioHangService {

    private final GioHangRepository gioHangRepository;

    private final NguoiDungRepository nguoiDungRepository;

    private final SanPhamRepository sanPhamRepository;

    private final MaGiamGiaRepository maGiamGiaRepository;

    private final VoucherKhachHangRepository voucherKhachHangRepository;

    private final CakeDesignPricingService cakeDesignPricingService;

    // ─── LẤY GIỎ HÀNG CỦA USER (tạo mới nếu chưa có) ──────────────────────
    @Transactional
    public GioHangDto.GioHangResponse layGioHang(String email) {
        NguoiDung nguoiDung = timNguoiDung(email);
        GioHang gioHang = layHoacTaoGioHang(nguoiDung);
        return mapToGioHangResponse(gioHang);
    }

    // ─── THÊM SẢN PHẨM VÀO GIỎ ─────────────────────────────────────────────
    @Transactional
    public GioHangDto.GioHangResponse themVaoGio(String email, GioHangDto.ThemVaoGioRequest request) {
        NguoiDung nguoiDung = timNguoiDung(email);
        GioHang gioHang = layHoacTaoGioHang(nguoiDung);

        SanPham sanPham = sanPhamRepository.findById(request.getSanPhamId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + request.getSanPhamId()));

        if (!"DANG_BAN".equals(sanPham.getTrangThai())) {
            throw new BusinessException("Sản phẩm \"" + sanPham.getTenSanPham() + "\" hiện không còn bán.");
        }

        // Bánh 3D tùy chỉnh (có thietKeBanhJson) là hàng làm theo yêu cầu, không áp dụng
        // kiểm tra tồn kho như sản phẩm bán sẵn thông thường. Giá của nó KHÔNG lấy trực
        // tiếp từ request.getDonGiaTuyChinh() (client gửi lên, có thể bị sửa qua DevTools),
        // mà BE tự tính lại từ chính JSON thiết kế (size + số tầng + phụ kiện tra giá
        // thật trong DB) - xem CakeDesignPricingService.
        boolean laBanhTuyChinh = request.getThietKeBanhJson() != null
                && !request.getThietKeBanhJson().isBlank();
        BigDecimal giaBanhTuyChinh = laBanhTuyChinh
                ? cakeDesignPricingService.tinhGiaChuan(request.getThietKeBanhJson())
                : null;

        if (!laBanhTuyChinh && sanPham.getSoLuongTon() <= 0) {
            throw new BusinessException("Sản phẩm \"" + sanPham.getTenSanPham() + "\" đã hết hàng.");
        }

        // Kiểm tra xem sản phẩm đã có trong giỏ chưa (không tính thiết kế 3D)
        Optional<ChiTietGioHang> chiTietTonTai = gioHang.getChiTietGioHangs().stream()
                .filter(ct -> ct.getSanPham().getId().equals(sanPham.getId())
                        && Objects.equals(ct.getThietKeBanhJson(), request.getThietKeBanhJson()))
                .findFirst();

        if (chiTietTonTai.isPresent()) {
            // Tăng số lượng nếu đã có
            ChiTietGioHang chiTiet = chiTietTonTai.get();
            int soLuongMoi = chiTiet.getSoLuong() + request.getSoLuong();
            if (!laBanhTuyChinh && soLuongMoi > sanPham.getSoLuongTon()) {
                throw new BusinessException("Số lượng vượt quá tồn kho! Còn lại: " + sanPham.getSoLuongTon());
            }
            chiTiet.setSoLuong(soLuongMoi);
            if (laBanhTuyChinh) {
                chiTiet.setDonGiaTuyChinh(giaBanhTuyChinh);
            }
        } else {
            // Thêm mới
            if (!laBanhTuyChinh && request.getSoLuong() > sanPham.getSoLuongTon()) {
                throw new BusinessException("Số lượng vượt quá tồn kho! Còn lại: " + sanPham.getSoLuongTon());
            }
            ChiTietGioHang chiTietMoi = ChiTietGioHang.builder()
                    .gioHang(gioHang)
                    .sanPham(sanPham)
                    .soLuong(request.getSoLuong())
                    .thietKeBanhJson(request.getThietKeBanhJson())
                    .donGiaTuyChinh(giaBanhTuyChinh)
                    .build();
            gioHang.getChiTietGioHangs().add(chiTietMoi);
        }

        gioHangRepository.save(gioHang);
        return mapToGioHangResponse(gioHang);
    }

    // ─── CẬP NHẬT SỐ LƯỢNG SẢN PHẨM TRONG GIỎ ─────────────────────────────
    @Transactional
    public GioHangDto.GioHangResponse capNhatSoLuong(String email, Long chiTietId, GioHangDto.CapNhatSoLuongRequest request) {
        NguoiDung nguoiDung = timNguoiDung(email);
        GioHang gioHang = layHoacTaoGioHang(nguoiDung);

        ChiTietGioHang chiTiet = gioHang.getChiTietGioHangs().stream()
                .filter(ct -> ct.getId().equals(chiTietId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng!"));

        if (request.getSoLuong() <= 0) {
            // Xóa nếu số lượng <= 0
            gioHang.getChiTietGioHangs().remove(chiTiet);
        } else {
            boolean laBanhTuyChinh = chiTiet.getDonGiaTuyChinh() != null;
            if (!laBanhTuyChinh && request.getSoLuong() > chiTiet.getSanPham().getSoLuongTon()) {
                throw new BusinessException("Số lượng vượt quá tồn kho! Còn lại: " + chiTiet.getSanPham().getSoLuongTon());
            }
            chiTiet.setSoLuong(request.getSoLuong());
        }

        gioHangRepository.save(gioHang);
        return mapToGioHangResponse(gioHang);
    }

    // ─── XÓA 1 SẢN PHẨM KHỎI GIỎ ───────────────────────────────────────────
    @Transactional
    public GioHangDto.GioHangResponse xoaKhoiGio(String email, Long chiTietId) {
        NguoiDung nguoiDung = timNguoiDung(email);
        GioHang gioHang = layHoacTaoGioHang(nguoiDung);

        boolean removed = gioHang.getChiTietGioHangs()
                .removeIf(ct -> ct.getId().equals(chiTietId));

        if (!removed) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng!");
        }

        gioHangRepository.save(gioHang);
        return mapToGioHangResponse(gioHang);
    }

    // ─── XÓA TOÀN BỘ GIỎ HÀNG ──────────────────────────────────────────────
    @Transactional
    public void xoaToanBoGio(String email) {
        NguoiDung nguoiDung = timNguoiDung(email);
        GioHang gioHang = layHoacTaoGioHang(nguoiDung);
        gioHang.getChiTietGioHangs().clear();
        gioHang.setMaGiamGia(null);
        gioHang.setVoucherKhachHang(null);
        gioHangRepository.save(gioHang);
    }

    // ─── ÁP DỤNG MÃ GIẢM GIÁ VÀO GIỎ HÀNG ──────────────────────────────────
    @Transactional
    public GioHangDto.GioHangResponse apDungMaGiamGia(String email, GioHangDto.ApplyDiscountRequest request) {
        NguoiDung nguoiDung = timNguoiDung(email);
        GioHang gioHang = layHoacTaoGioHang(nguoiDung);

        if (gioHang.getChiTietGioHangs().isEmpty()) {
            throw new BusinessException("Giỏ hàng đang trống, không thể áp dụng mã giảm giá!");
        }

        MaGiamGia maGiamGia = maGiamGiaRepository.findByMaCode(request.getMaCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại!"));

        BigDecimal tongTienHang = tinhTongTienHang(gioHang);
        kiemTraMaGiamGiaHopLe(maGiamGia, tongTienHang);

        // Mỗi đơn chỉ áp dụng 1 ưu đãi — áp mã giảm giá thì gỡ voucher cá nhân (nếu có)
        gioHang.setVoucherKhachHang(null);
        gioHang.setMaGiamGia(maGiamGia);
        gioHangRepository.save(gioHang);
        return mapToGioHangResponse(gioHang);
    }

    // ─── GỠ MÃ GIẢM GIÁ KHỎI GIỎ HÀNG ──────────────────────────────────────
    @Transactional
    public GioHangDto.GioHangResponse xoaMaGiamGia(String email) {
        NguoiDung nguoiDung = timNguoiDung(email);
        GioHang gioHang = layHoacTaoGioHang(nguoiDung);
        gioHang.setMaGiamGia(null);
        gioHangRepository.save(gioHang);
        return mapToGioHangResponse(gioHang);
    }

    // ─── ÁP DỤNG VOUCHER CÁ NHÂN (ĐỔI BẰNG ĐIỂM) VÀO GIỎ HÀNG ───────────────
    @Transactional
    public GioHangDto.GioHangResponse apDungVoucherKhachHang(String email, GioHangDto.ApplyVoucherKhachHangRequest request) {
        NguoiDung nguoiDung = timNguoiDung(email);
        GioHang gioHang = layHoacTaoGioHang(nguoiDung);

        if (gioHang.getChiTietGioHangs().isEmpty()) {
            throw new BusinessException("Giỏ hàng đang trống, không thể áp dụng voucher!");
        }

        VoucherKhachHang voucher = voucherKhachHangRepository
                .findByIdAndKhachHang(request.getVoucherKhachHangId(), nguoiDung)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher này!"));

        BigDecimal tongTienHang = tinhTongTienHang(gioHang);
        kiemTraVoucherKhachHangHopLe(voucher, tongTienHang);

        // Mỗi đơn chỉ áp dụng 1 ưu đãi — áp voucher cá nhân thì gỡ mã giảm giá (nếu có)
        gioHang.setMaGiamGia(null);
        gioHang.setVoucherKhachHang(voucher);
        gioHangRepository.save(gioHang);
        return mapToGioHangResponse(gioHang);
    }

    // ─── GỠ VOUCHER CÁ NHÂN KHỎI GIỎ HÀNG ───────────────────────────────────
    @Transactional
    public GioHangDto.GioHangResponse xoaVoucherKhachHang(String email) {
        NguoiDung nguoiDung = timNguoiDung(email);
        GioHang gioHang = layHoacTaoGioHang(nguoiDung);
        gioHang.setVoucherKhachHang(null);
        gioHangRepository.save(gioHang);
        return mapToGioHangResponse(gioHang);
    }

    // ─── HELPER: Kiểm tra mã giảm giá còn hợp lệ để áp dụng không ──────────
    private void kiemTraMaGiamGiaHopLe(MaGiamGia maGiamGia, BigDecimal tongTienHang) {
        if (!Boolean.TRUE.equals(maGiamGia.getHoatDong())) {
            throw new BusinessException("Mã giảm giá này hiện không còn hoạt động!");
        }
        if (maGiamGia.getNgayHetHan() != null && maGiamGia.getNgayHetHan().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Mã giảm giá này đã hết hạn!");
        }
        if (maGiamGia.getSoLuotToiDa() != null
                && maGiamGia.getSoLuotDaDung() != null
                && maGiamGia.getSoLuotDaDung() >= maGiamGia.getSoLuotToiDa()) {
            throw new BusinessException("Mã giảm giá này đã hết lượt sử dụng!");
        }
        if (maGiamGia.getDonHangToiThieu() != null
                && tongTienHang.compareTo(maGiamGia.getDonHangToiThieu()) < 0) {
            throw new BusinessException(
                    "Đơn hàng chưa đạt giá trị tối thiểu " + maGiamGia.getDonHangToiThieu()
                            + " để áp dụng mã này!");
        }
    }

    // ─── HELPER: Tính số tiền được giảm theo loại mã ───────────────────────
    private BigDecimal tinhSoTienGiam(MaGiamGia maGiamGia, BigDecimal tongTienHang) {
        if (maGiamGia == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal soTienGiam;
        if ("PHAN_TRAM".equals(maGiamGia.getLoaiGiamGia())) {
            soTienGiam = tongTienHang.multiply(maGiamGia.getGiaTriGiam())
                    .divide(BigDecimal.valueOf(100));
        } else {
            soTienGiam = maGiamGia.getGiaTriGiam();
        }
        // Không cho số tiền giảm vượt quá tổng tiền hàng
        return soTienGiam.min(tongTienHang);
    }

    // ─── HELPER: Tính tổng tiền hàng (chưa gồm ship, chưa trừ giảm giá) ────
    private BigDecimal tinhTongTienHang(GioHang gioHang) {
        return gioHang.getChiTietGioHangs().stream()
                .map(ct -> donGiaHieuLuc(ct).multiply(BigDecimal.valueOf(ct.getSoLuong())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ─── HELPER: Đơn giá thực tế của 1 item trong giỏ (ưu tiên giá bánh 3D tùy chỉnh) ─
    private BigDecimal donGiaHieuLuc(ChiTietGioHang ct) {
        return ct.getDonGiaTuyChinh() != null ? ct.getDonGiaTuyChinh() : ct.getSanPham().getDonGia();
    }

    // ─── HELPER: Kiểm tra voucher cá nhân còn hợp lệ để áp dụng không ───────
    private void kiemTraVoucherKhachHangHopLe(VoucherKhachHang voucher, BigDecimal tongTienHang) {
        if (!"CHUA_SU_DUNG".equals(voucher.getTrangThai())) {
            throw new BusinessException("Voucher này đã được sử dụng hoặc không còn hiệu lực!");
        }
        if (voucher.getNgayHetHan() != null && voucher.getNgayHetHan().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Voucher này đã hết hạn!");
        }
        if (voucher.getDonHangToiThieu() != null
                && tongTienHang.compareTo(voucher.getDonHangToiThieu()) < 0) {
            throw new BusinessException(
                    "Đơn hàng chưa đạt giá trị tối thiểu " + voucher.getDonHangToiThieu()
                            + " để áp dụng voucher này!");
        }
    }

    // ─── HELPER: Tính số tiền được giảm theo voucher cá nhân ────────────────
    private BigDecimal tinhSoTienGiamVoucher(VoucherKhachHang voucher, BigDecimal tongTienHang) {
        if (voucher == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal soTienGiam;
        if ("PHAN_TRAM".equals(voucher.getLoaiGiam())) {
            soTienGiam = tongTienHang.multiply(voucher.getGiaTriGiam())
                    .divide(BigDecimal.valueOf(100));
        } else {
            soTienGiam = voucher.getGiaTriGiam();
        }
        return soTienGiam.min(tongTienHang);
    }

    // ─── HELPER: Tìm người dùng ──────────────────────────────────────────────
    private NguoiDung timNguoiDung(String email) {
        return nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + email));
    }

    // ─── HELPER: Lấy hoặc tạo giỏ hàng ─────────────────────────────────────
    private GioHang layHoacTaoGioHang(NguoiDung nguoiDung) {
        return gioHangRepository.findByKhachHang(nguoiDung)
                .orElseGet(() -> {
                    GioHang moi = GioHang.builder()
                            .khachHang(nguoiDung)
                            .build();
                    return gioHangRepository.save(moi);
                });
    }

    // ─── HELPER: Chuyển đổi Entity → DTO ────────────────────────────────────
    private GioHangDto.GioHangResponse mapToGioHangResponse(GioHang gioHang) {
        List<GioHangDto.ChiTietGioHangResponse> items = gioHang.getChiTietGioHangs().stream()
                .map(this::mapToChiTietResponse)
                .collect(Collectors.toList());

        BigDecimal tongTienHang = items.stream()
                .map(GioHangDto.ChiTietGioHangResponse::getThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int tongSoLuong = items.stream()
                .mapToInt(GioHangDto.ChiTietGioHangResponse::getSoLuong)
                .sum();

        // Miễn phí ship cho đơn >= 500,000đ
        BigDecimal phiShip = tongTienHang.compareTo(BigDecimal.valueOf(500000)) >= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(30000);

        // Mã giảm giá đang áp dụng ở giỏ (nếu có) — nếu không còn hợp lệ nữa
        // (hết hạn/hết lượt/dưới mức tối thiểu do giỏ hàng vừa bị thay đổi) thì
        // tự động gỡ ra để tránh hiển thị sai cho khách.
        MaGiamGia maGiamGia = gioHang.getMaGiamGia();
        VoucherKhachHang voucherKhachHang = gioHang.getVoucherKhachHang();
        BigDecimal soTienGiam = BigDecimal.ZERO;

        if (maGiamGia != null) {
            try {
                kiemTraMaGiamGiaHopLe(maGiamGia, tongTienHang);
                soTienGiam = tinhSoTienGiam(maGiamGia, tongTienHang);
            } catch (BusinessException e) {
                gioHang.setMaGiamGia(null);
                gioHangRepository.save(gioHang);
                maGiamGia = null;
            }
        } else if (voucherKhachHang != null) {
            try {
                kiemTraVoucherKhachHangHopLe(voucherKhachHang, tongTienHang);
                soTienGiam = tinhSoTienGiamVoucher(voucherKhachHang, tongTienHang);
            } catch (BusinessException e) {
                gioHang.setVoucherKhachHang(null);
                gioHangRepository.save(gioHang);
                voucherKhachHang = null;
            }
        }

        BigDecimal tongThanhToan = tongTienHang.subtract(soTienGiam).add(phiShip);

        GioHangDto.GioHangResponse response = new GioHangDto.GioHangResponse();
        response.setId(gioHang.getId());
        response.setItems(items);
        response.setTongSoLuong(tongSoLuong);
        response.setTongTienHang(tongTienHang);
        response.setPhiShip(phiShip);
        if (maGiamGia != null) {
            response.setMaGiamGiaCode(maGiamGia.getMaCode());
            response.setLoaiGiamGia(maGiamGia.getLoaiGiamGia());
            response.setSoTienGiam(soTienGiam);
        } else if (voucherKhachHang != null) {
            response.setVoucherKhachHangId(voucherKhachHang.getId());
            response.setTenVoucherKhachHang(voucherKhachHang.getTenVoucher());
            response.setLoaiGiamGia(voucherKhachHang.getLoaiGiam());
            response.setSoTienGiam(soTienGiam);
        }
        response.setTongThanhToan(tongThanhToan);
        response.setNgayCapNhat(gioHang.getNgayCapNhat());
        return response;
    }

    private GioHangDto.ChiTietGioHangResponse mapToChiTietResponse(ChiTietGioHang ct) {
        GioHangDto.ChiTietGioHangResponse item = new GioHangDto.ChiTietGioHangResponse();
        item.setId(ct.getId());
        item.setSanPhamId(ct.getSanPham().getId());
        item.setTenSanPham(ct.getSanPham().getTenSanPham());
        item.setAnhSanPham(ct.getSanPham().getAnhSanPham());
        item.setTenDanhMuc(ct.getSanPham().getDanhMuc() != null
                ? ct.getSanPham().getDanhMuc().getTenDanhMuc() : null);
        item.setDonGia(donGiaHieuLuc(ct));
        item.setSoLuong(ct.getSoLuong());
        item.setThanhTien(donGiaHieuLuc(ct).multiply(BigDecimal.valueOf(ct.getSoLuong())));
        item.setThietKeBanhJson(ct.getThietKeBanhJson());
        item.setNgayTao(ct.getNgayTao());
        return item;
    }
}