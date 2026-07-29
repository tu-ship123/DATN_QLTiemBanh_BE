package com.poly.cake.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.cake.dto.GioHangDto;
import com.poly.cake.dto.DatHangDto;
import com.poly.cake.dto.DatHangXuLyDto;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.ChiTietDonHang;
import com.poly.cake.entity.GioHang;
import com.poly.cake.entity.MaGiamGia;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.SanPham;
import com.poly.cake.entity.ThanhToan;
import com.poly.cake.entity.TrangThaiDonHang;
import com.poly.cake.entity.VoucherKhachHang;
import com.poly.cake.exception.NgoaiLeNghiepVu;
import com.poly.cake.exception.NgoaiLeCamTruyCap;
import com.poly.cake.exception.NgoaiLeKhongTimThayTaiNguyen;
import com.poly.cake.repository.DonHangRepository;
import com.poly.cake.repository.ChiTietDonHangRepository;
import com.poly.cake.repository.GioHangRepository;
import com.poly.cake.repository.MaGiamGiaRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.SanPhamRepository;
import com.poly.cake.repository.ThanhToanRepository;
import com.poly.cake.repository.VoucherKhachHangRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatHangService {

    private final DonHangRepository donHangRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final SanPhamRepository sanPhamRepository;
    private final ThongBaoService notificationService;
    private final ThanhToanRepository thanhToanRepository;
    private final TonKhoService inventoryService;
    private final GioHangRepository gioHangRepository;
    private final MaGiamGiaRepository maGiamGiaRepository;
    private final VoucherKhachHangRepository voucherKhachHangRepository;
    private final ThietKeBanhValidator cakeDesignValidator;
    private final DichVuEmail emailService;
    private final HoaDonPdfService invoicePdfService;
    private final DichVuWebhookDiscord discordWebhookService;

    // DF_ST05 – Dùng lại logic thêm-vào-giỏ có sẵn (đã xử lý validate tồn kho,
    // bánh 3D tùy chỉnh, gộp số lượng...) để implement tính năng "Đặt lại đơn cũ".
    private final GioHangService gioHangService;

    // T102 – Tính % phụ thu tự động theo ngày giao hàng (dịp đặc biệt)
    private final PhuThuDonHangService phuThuDonHangService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. TẠO ĐƠN HÀNG (CHECKOUT)
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public DatHangDto.Response createOrder(DatHangDto.Request request, String emailNguoiDung) {

        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailNguoiDung)
                .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen("Không tìm thấy thông tin tài khoản!"));

        if (request.getNgayGiaoHang() == null || request.getNgayGiaoHang().isBefore(LocalDate.now())) {
            throw new NgoaiLeNghiepVu("Ngày giao hàng không hợp lệ! Phải chọn từ ngày hôm nay trở đi.");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new NgoaiLeNghiepVu("Giỏ hàng trống! Không thể đặt hàng.");
        }

        Map<String, Object> designMap = cakeDesignValidator.validateAndParse(request.getCakeDesignJson());

        double tongTienHang = request.getItems().stream()
                .mapToDouble(item -> item.getDonGia() * item.getSoLuong())
                .sum();

        GioHang gioHang = gioHangRepository.findByKhachHang(khachHang).orElse(null);
        MaGiamGia maGiamGiaApDung = gioHang != null ? gioHang.getMaGiamGia() : null;
        VoucherKhachHang voucherApDung = (gioHang != null && maGiamGiaApDung == null)
                ? gioHang.getVoucherKhachHang() : null;
        BigDecimal soTienGiam = BigDecimal.ZERO;

        if (maGiamGiaApDung != null) {
            kiemTraMaGiamGiaHopLe(maGiamGiaApDung, BigDecimal.valueOf(tongTienHang));
            soTienGiam = tinhSoTienGiam(maGiamGiaApDung, BigDecimal.valueOf(tongTienHang));
        } else if (voucherApDung != null) {
            kiemTraVoucherKhachHangHopLe(voucherApDung, BigDecimal.valueOf(tongTienHang));
            soTienGiam = tinhSoTienGiamVoucher(voucherApDung, BigDecimal.valueOf(tongTienHang));
        }

        double phiShip = (tongTienHang >= 500_000) ? 0.0 : 30_000.0;

        // ─────────────────────────────────────────────────────────────────────────────
        // T102 – LOGIC PHỤ THU DỊP ĐẶC BIỆT
        // Tính theo NGÀY GIAO HÀNG (request.getNgayGiaoHang()), không phải ngày đặt hàng,
        // vì bánh thường đặt trước nhiều ngày để giao đúng dịp (VD: đặt trước 1 tuần
        // để giao đúng Valentine). Nếu nhiều dịp đặc biệt trùng ngày thì cộng dồn %.
        // ─────────────────────────────────────────────────────────────────────────────
        BigDecimal phanTramPhuThu = phuThuDonHangService.tinhPhanTramPhuThu(request.getNgayGiaoHang());
        BigDecimal soTienPhuThu = BigDecimal.valueOf(tongTienHang)
                .multiply(phanTramPhuThu)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (soTienPhuThu.compareTo(BigDecimal.ZERO) > 0) {
            log.info("T102 - Đơn hàng có ngày giao {} rơi vào dịp đặc biệt, phụ thu {}%, số tiền đội lên: {}",
                    request.getNgayGiaoHang(), phanTramPhuThu, soTienPhuThu);
        }

        BigDecimal tongTienThanhToan = BigDecimal.valueOf(tongTienHang)
                .subtract(soTienGiam)
                .add(BigDecimal.valueOf(phiShip))
                .add(soTienPhuThu);
        // ─────────────────────────────────────────────────────────────────────────────

        String ghiChuCuoiCung;
        if (designMap != null) {
            ghiChuCuoiCung = cakeDesignValidator.buildGhiChuKichThuoc(designMap, request.getGhiChu());
        } else {
            ghiChuCuoiCung = request.getGhiChu();
        }

        DonHang donHang = new DonHang();
        donHang.setKhachHang(khachHang);
        donHang.setDiaChiGiao(request.getDiaChiGiaoHang());
        donHang.setNgayGiaoDuKien(request.getNgayGiaoHang().atTime(12, 0));
        donHang.setTongTien(tongTienThanhToan);
        donHang.setSoTienPhuThu(soTienPhuThu);
        donHang.setGhiChu(ghiChuCuoiCung);
        donHang.setTrangThai(TrangThaiDonHang.CHO_XAC_NHAN);
        donHang.setNguonDon("ONLINE");

        if (maGiamGiaApDung != null) {
            donHang.setMaGiamGia(maGiamGiaApDung);
        } else if (voucherApDung != null) {
            donHang.setVoucherKhachHang(voucherApDung);
        }

        if (designMap != null) {
            try {
                donHang.setThietKeBanhJson(objectMapper.writeValueAsString(designMap));
            } catch (Exception e) {
                donHang.setThietKeBanhJson(request.getCakeDesignJson());
            }
        } else {
            request.getItems().stream()
                    .map(DatHangDto.OrderItemRequest::getThietKeBanhJson)
                    .filter(json -> json != null && !json.isBlank())
                    .findFirst()
                    .ifPresent(donHang::setThietKeBanhJson);
        }

        DonHang savedDonHang = donHangRepository.save(donHang);

        List<ChiTietDonHang> chiTietList = request.getItems().stream().map(itemDto -> {
            SanPham sanPham = sanPhamRepository.findById(itemDto.getSanPhamId())
                    .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen("Sản phẩm không tồn tại!"));
            ChiTietDonHang chiTiet = new ChiTietDonHang();
            chiTiet.setDonHang(savedDonHang);
            chiTiet.setSanPham(sanPham);
            chiTiet.setSoLuong(itemDto.getSoLuong());
            chiTiet.setDonGiaTaiThoiDiem(BigDecimal.valueOf(itemDto.getDonGia()));
            chiTiet.setThietKeBanhJson(itemDto.getThietKeBanhJson());
            return chiTiet;
        }).collect(Collectors.toList());

        chiTietDonHangRepository.saveAll(chiTietList);
        savedDonHang.setChiTietDonHangs(chiTietList);

        if (maGiamGiaApDung != null) {
            maGiamGiaApDung.setSoLuotDaDung(
                    (maGiamGiaApDung.getSoLuotDaDung() == null ? 0 : maGiamGiaApDung.getSoLuotDaDung()) + 1);
            maGiamGiaRepository.save(maGiamGiaApDung);
            gioHang.setMaGiamGia(null);
            gioHangRepository.save(gioHang);
        } else if (voucherApDung != null) {
            voucherApDung.setTrangThai("DA_SU_DUNG");
            voucherApDung.setNgaySuDung(LocalDateTime.now());
            voucherKhachHangRepository.save(voucherApDung);

            MaGiamGia maGiamGiaGoc = voucherApDung.getMaGiamGiaGoc();
            if (maGiamGiaGoc != null) {
                maGiamGiaGoc.setSoLuotDaDung(
                        (maGiamGiaGoc.getSoLuotDaDung() == null ? 0 : maGiamGiaGoc.getSoLuotDaDung()) + 1);
                maGiamGiaRepository.save(maGiamGiaGoc);
            }
            gioHang.setVoucherKhachHang(null);
            gioHangRepository.save(gioHang);
        }

        String loiNhanAdmin = designMap != null
                ? "🎂 [BÁNH 3D] Đơn hàng mới HD-" + savedDonHang.getId() + " có thiết kế bánh 3D!"
                : "TING TING! Có đơn hàng mới được đặt: HD-" + savedDonHang.getId();
        notificationService.notifyNewOrderToAdmins(loiNhanAdmin);

        try {
            byte[] pdfHoaDon = invoicePdfService.generateInvoicePdf(savedDonHang);
            emailService.sendOrderConfirmationEmail(
                    khachHang.getEmail(), khachHang.getHoTen(), "HD-" + savedDonHang.getId(), pdfHoaDon);
        } catch (Exception e) {
            log.error("Gửi email xác nhận đơn hàng HD-{} thất bại: {}", savedDonHang.getId(), e.getMessage(), e);
        }

        try {
            discordWebhookService.sendNewOrderNotification(
                    savedDonHang.getId(),
                    khachHang.getHoTen(),
                    tongTienThanhToan.doubleValue());
        } catch (Exception e) {
            log.warn("Gửi Discord thông báo đơn mới HD-{} thất bại: {}", savedDonHang.getId(), e.getMessage());
        }

        return mapToResponseDto(savedDonHang, false);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CÁC HÀM CÒN LẠI GIỮ NGUYÊN
    // ═══════════════════════════════════════════════════════════════════════════
    public List<DatHangDto.Response> getOrdersByUser(String email) {
        NguoiDung khachHang = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen("Tài khoản không tồn tại."));
        return donHangRepository.findByKhachHangWithDetails(khachHang)
                .stream().map(d -> mapToResponseDto(d, false)).collect(Collectors.toList());
    }

    public List<DatHangDto.Response> getAllOrders() {
        return donHangRepository.findAll().stream()
                .map(d -> mapToResponseDto(d, true)).collect(Collectors.toList());
    }

    public DatHangDto.Response getOrderById(Long id, String email, String role) {
        DonHang donHang = donHangRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen("Không tìm thấy đơn hàng: " + id));

        boolean laKhachHang = "ROLE_KHACH_HANG".equals(role) || "KHACH_HANG".equals(role);

        if (laKhachHang) {
            if (donHang.getKhachHang() == null || !donHang.getKhachHang().getEmail().equals(email)) {
                throw new NgoaiLeCamTruyCap("Bạn không có quyền xem đơn hàng này!");
            }
        }
        return mapToResponseDto(donHang, !laKhachHang);
    }

    public byte[] getInvoicePdf(Long id, String email, String role) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen("Không tìm thấy đơn hàng: " + id));

        if ("ROLE_KHACH_HANG".equals(role) || "KHACH_HANG".equals(role)) {
            if (donHang.getKhachHang() == null || !donHang.getKhachHang().getEmail().equals(email)) {
                throw new NgoaiLeCamTruyCap("Bạn không có quyền xem hóa đơn của đơn hàng này!");
            }
        }
        return invoicePdfService.generateInvoicePdf(donHang);
    }

    @Transactional
    public DatHangDto.Response processOrder(Long id, DatHangXuLyDto request, String emailNhanVien) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen("Đơn hàng không tồn tại."));
        NguoiDung nhanVien = nguoiDungRepository.findByEmail(emailNhanVien)
                .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen("Không tìm thấy thông tin nhân viên xử lý!"));

        donHang.setNhanVien(nhanVien);

        TrangThaiDonHang trangThaiMoi;
        try {
            trangThaiMoi = TrangThaiDonHang.valueOf(request.getTrangThai().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NgoaiLeNghiepVu("Trạng thái đơn hàng không hợp lệ!");
        }

        TrangThaiDonHang trangThaiHienTai = donHang.getTrangThai();

        if (trangThaiHienTai == TrangThaiDonHang.HOAN_THANH
                || trangThaiHienTai == TrangThaiDonHang.DA_HUY
                || trangThaiHienTai == TrangThaiDonHang.DA_GIAO
                || trangThaiHienTai == TrangThaiDonHang.DA_HOAN_TIEN) {
            throw new NgoaiLeNghiepVu("Đơn hàng đã chốt (Giao/Hủy/Hoàn tiền) thì không thể thay đổi trạng thái được nữa!");
        }

        if (trangThaiHienTai == TrangThaiDonHang.DANG_GIAO &&
                (trangThaiMoi == TrangThaiDonHang.CHO_XAC_NHAN || trangThaiMoi == TrangThaiDonHang.DANG_LAM)) {
            throw new NgoaiLeNghiepVu("Đơn hàng đang giao, không thể lùi trạng thái!");
        }

        donHang.setTrangThai(trangThaiMoi);

        if (trangThaiMoi == TrangThaiDonHang.DA_HUY) {
            if (request.getLyDoHuy() == null || request.getLyDoHuy().trim().isEmpty()) {
                throw new NgoaiLeNghiepVu("Bắt buộc phải nhập lý do khi hủy đơn hàng!");
            }
            donHang.setLyDoHuy(request.getLyDoHuy());

            // Nếu đơn hủy sau khi đã trừ kho (đã từng đạt SAN_SANG trở lên) thì
            // phải cộng trả lại tồn kho, tránh thất thoát hàng trong kho.
            if (Boolean.TRUE.equals(donHang.getDaTruTonKho())) {
                for (ChiTietDonHang ct : donHang.getChiTietDonHangs()) {
                    sanPhamRepository.congLaiSoLuongTon(ct.getSanPham().getId(), ct.getSoLuong());
                }
                donHang.setDaTruTonKho(false);
            }
        }

        // NGHIỆP VỤ TRỪ TỒN KHO: chỉ trừ đúng 1 LẦN, tại thời điểm đơn hàng
        // chuyển sang trạng thái SAN_SANG (sẵn sàng giao) — không trừ ngay lúc
        // đặt hàng/xác nhận/thanh toán, vì trước SAN_SANG đơn vẫn có thể bị hủy
        // hoặc thay đổi. Cờ daTruTonKho đảm bảo không bị trừ lặp lại nếu đơn
        // được set lại trạng thái SAN_SANG nhiều lần.
        if (trangThaiMoi == TrangThaiDonHang.SAN_SANG && !Boolean.TRUE.equals(donHang.getDaTruTonKho())) {
            inventoryService.truTonKhoTheoDonHang(donHang);
            donHang.setDaTruTonKho(true);
        }

        DonHang updatedDonHang = donHangRepository.save(donHang);

        if (updatedDonHang.getKhachHang() != null &&
                !"khachvanglai@gmail.com".equals(updatedDonHang.getKhachHang().getEmail())) {
            String loiNhan = "Đơn hàng HD-" + id + " của bạn vừa chuyển sang trạng thái: " + trangThaiMoi.name();
            if (trangThaiMoi == TrangThaiDonHang.DA_HUY) {
                loiNhan += ". Lý do: " + request.getLyDoHuy();
            }
            notificationService.notifyOrderStatusToUser(updatedDonHang.getKhachHang().getEmail(), loiNhan);
        }
        return mapToResponseDto(updatedDonHang, true);
    }

    private static final EnumSet<TrangThaiDonHang> TRANG_THAI_CON_DUOC_TU_HUY = EnumSet.of(
            TrangThaiDonHang.CHO_XAC_NHAN, TrangThaiDonHang.DA_XAC_NHAN);

    @Transactional
    public DatHangDto.Response cancelOrder(Long id, String emailNguoiDung) {
        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailNguoiDung)
                .orElseThrow(() -> new NgoaiLeNghiepVu("Tài khoản không hợp lệ."));

        DonHang donHang = donHangRepository.findByIdAndKhachHang(id, khachHang)
                .orElseThrow(() -> new NgoaiLeCamTruyCap(
                        "Đơn hàng không thuộc quyền sở hữu của bạn hoặc không tồn tại."));

        TrangThaiDonHang trangThaiHienTai = donHang.getTrangThai();

        if (!TRANG_THAI_CON_DUOC_TU_HUY.contains(trangThaiHienTai)) {
            throw new NgoaiLeNghiepVu(
                    "Đơn hàng đang ở trạng thái \"" + trangThaiHienTai.name() +
                            "\", đã được cửa hàng xử lý nên không thể tự hủy vào lúc này. " +
                            "Vui lòng liên hệ cửa hàng nếu cần hỗ trợ thêm!");
        }

        Optional<ThanhToan> thanhToanOpt = thanhToanRepository.findByDonHang(donHang);
        boolean daThanhToan = thanhToanOpt.isPresent() && "THANH_CONG".equals(thanhToanOpt.get().getTrangThai());
        BigDecimal soTienHoan = null;

        if (daThanhToan) {
            ThanhToan thanhToan = thanhToanOpt.get();
            soTienHoan = thanhToan.getSoTien();

            thanhToan.setTrangThai("DA_HOAN_TIEN");
            thanhToanRepository.save(thanhToan);

            donHang.setTrangThai(TrangThaiDonHang.DA_HOAN_TIEN);
            donHang.setLyDoHuy("Khách hàng tự hủy trên web - đã hoàn tiền "
                    + soTienHoan + "đ vào " + LocalDateTime.now());
        } else {
            donHang.setTrangThai(TrangThaiDonHang.DA_HUY);
            donHang.setLyDoHuy("Khách hàng tự hủy trên web");
        }

        // Cộng trả tồn kho CHỈ khi đơn ĐÃ thực sự bị trừ kho trước đó (tức đã từng
        // đạt trạng thái SAN_SANG). Khách chỉ được tự hủy khi đơn còn ở CHO_XAC_NHAN
        // hoặc DA_XAC_NHAN (xem TRANG_THAI_CON_DUOC_TU_HUY) — tức là TRƯỚC thời điểm
        // trừ kho (SAN_SANG) — nên bình thường sẽ không cần cộng trả gì cả. Vẫn giữ
        // guard này để phòng vệ nếu luồng hủy sau này được nới thêm trạng thái khác.
        if (Boolean.TRUE.equals(donHang.getDaTruTonKho())) {
            for (ChiTietDonHang ct : donHang.getChiTietDonHangs()) {
                sanPhamRepository.congLaiSoLuongTon(ct.getSanPham().getId(), ct.getSoLuong());
            }
            donHang.setDaTruTonKho(false);
        }

        DonHang updatedDonHang = donHangRepository.save(donHang);

        String thongBaoKhach = "Đơn hàng HD-" + id + " của bạn đã được hủy thành công."
                + (daThanhToan ? " Số tiền " + soTienHoan + "đ sẽ được hoàn lại trong 1-3 ngày làm việc." : "");
        notificationService.notifyOrderStatusToUser(emailNguoiDung, thongBaoKhach);
        notificationService.notifyNewOrderToAdmins(
                "❌ Khách hàng đã tự hủy đơn HD-" + id + (daThanhToan ? " (CẦN ĐỐI SOÁT HOÀN TIỀN)" : ""));

        try {
            emailService.sendOrderCancellationEmail(
                    khachHang.getEmail(), khachHang.getHoTen(), "HD-" + id,
                    updatedDonHang.getLyDoHuy(), soTienHoan);
        } catch (Exception e) {
            log.error("Gửi email hủy đơn HD-{} thất bại: {}", id, e.getMessage(), e);
        }

        try {
            discordWebhookService.sendOrderCancelledNotification(
                    id,
                    khachHang.getHoTen(),
                    updatedDonHang.getLyDoHuy(),
                    daThanhToan);
        } catch (Exception e) {
            log.warn("Gửi Discord thông báo hủy đơn HD-{} thất bại: {}", id, e.getMessage());
        }

        return mapToResponseDto(updatedDonHang, false);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DF_ST05 – "ĐẶT LẠI ĐƠN CŨ" (RE-ORDER)
    //
    // Bug cũ: tính năng này CHƯA hề tồn tại ở BE (không có endpoint/service nào),
    // nên khi FE build nút "Đặt lại đơn cũ" cho đơn có NHIỀU sản phẩm, không có
    // API nào trả về đủ toàn bộ sản phẩm để thêm lại vào giỏ -> bị thiếu sản phẩm.
    //
    // Fix: lặp qua TOÀN BỘ ChiTietDonHang của đơn cũ (không dừng lại ở sản phẩm
    // đầu tiên) và thêm từng sản phẩm vào giỏ hàng hiện tại của khách. Sản phẩm
    // nào không còn thêm được nữa (đã ngừng bán / hết hàng / bị xóa) sẽ được BỎ
    // QUA và liệt kê riêng, KHÔNG được để 1 sản phẩm lỗi làm hỏng toàn bộ thao tác.
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public DatHangDto.ReorderResponse datLaiDonHang(Long donHangId, String emailNguoiDung) {
        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailNguoiDung)
                .orElseThrow(() -> new NgoaiLeNghiepVu("Tài khoản không hợp lệ."));

        DonHang donHangCu = donHangRepository.findByIdAndKhachHang(donHangId, khachHang)
                .orElseThrow(() -> new NgoaiLeCamTruyCap(
                        "Đơn hàng không thuộc quyền sở hữu của bạn hoặc không tồn tại."));

        if (donHangCu.getChiTietDonHangs() == null || donHangCu.getChiTietDonHangs().isEmpty()) {
            throw new NgoaiLeNghiepVu("Đơn hàng này không có sản phẩm nào để đặt lại!");
        }

        List<String> sanPhamBiBoQua = new java.util.ArrayList<>();
        int soSanPhamDaThem = 0;

        for (ChiTietDonHang ct : donHangCu.getChiTietDonHangs()) {
            GioHangDto.ThemVaoGioRequest req = new GioHangDto.ThemVaoGioRequest();
            req.setSanPhamId(ct.getSanPham().getId());
            req.setSoLuong(ct.getSoLuong());
            req.setThietKeBanhJson(ct.getThietKeBanhJson());
            try {
                gioHangService.themVaoGio(emailNguoiDung, req);
                soSanPhamDaThem++;
            } catch (NgoaiLeNghiepVu | NgoaiLeKhongTimThayTaiNguyen e) {
                // Sản phẩm có thể đã ngừng bán/hết hàng/bị xóa kể từ lúc đặt đơn cũ.
                // Đây chính là nguyên nhân cần XỬ LÝ RIÊNG thay vì để lỗi văng ra
                // và làm mất luôn các sản phẩm khác chưa kịp thêm (bug gốc DF_ST05).
                sanPhamBiBoQua.add(ct.getSanPham().getTenSanPham() + " (" + e.getMessage() + ")");
            }
        }

        if (soSanPhamDaThem == 0) {
            throw new NgoaiLeNghiepVu(
                    "Không thể đặt lại đơn hàng này vì toàn bộ sản phẩm đều không còn khả dụng.");
        }

        GioHangDto.GioHangResponse gioHangMoi = gioHangService.layGioHang(emailNguoiDung);

        DatHangDto.ReorderResponse response = new DatHangDto.ReorderResponse();
        response.setGioHang(gioHangMoi);
        response.setSoSanPhamDaThem(soSanPhamDaThem);
        response.setSanPhamBiBoQua(sanPhamBiBoQua);
        return response;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DF_ST06 – "YÊU CẦU SỬA ĐƠN"
    //
    // Bug cũ: tính năng này CHƯA hề tồn tại ở BE (DTO DatHangDto.UpdateRequest đã
    // được khai báo sẵn nhưng KHÔNG được dùng ở bất kỳ service/controller nào)
    // -> yêu cầu sửa đơn của khách không được lưu lại và cũng không hề được báo
    // cho nhân viên biết, dù dưới bất kỳ hình thức nào.
    //
    // Fix: lưu lại nội dung khách muốn sửa (snapshot JSON) trực tiếp trên đơn
    // hàng, CHỈ cho phép gửi khi đơn đang ở trạng thái Chờ xác nhận, và quan
    // trọng nhất là BẮN THÔNG BÁO REALTIME cho toàn bộ nhân viên/admin ngay khi
    // khách gửi yêu cầu — đây chính là phần "đồng bộ tới nhân viên" bị thiếu.
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public DatHangDto.Response guiYeuCauSuaDon(Long donHangId, DatHangDto.UpdateRequest request, String emailNguoiDung) {
        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailNguoiDung)
                .orElseThrow(() -> new NgoaiLeNghiepVu("Tài khoản không hợp lệ."));

        DonHang donHang = donHangRepository.findByIdAndKhachHang(donHangId, khachHang)
                .orElseThrow(() -> new NgoaiLeCamTruyCap(
                        "Đơn hàng không thuộc quyền sở hữu của bạn hoặc không tồn tại."));

        if (donHang.getTrangThai() != TrangThaiDonHang.CHO_XAC_NHAN) {
            throw new NgoaiLeNghiepVu(
                    "Chỉ có thể gửi yêu cầu sửa đơn khi đơn đang ở trạng thái \"Chờ xác nhận\". " +
                            "Đơn hàng của bạn hiện đang ở trạng thái \"" + donHang.getTrangThai().name() +
                            "\", vui lòng liên hệ trực tiếp cửa hàng để được hỗ trợ.");
        }

        try {
            donHang.setYeuCauSuaDonJson(objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            throw new NgoaiLeNghiepVu("Không thể xử lý yêu cầu sửa đơn, vui lòng thử lại!");
        }
        donHang.setNgayYeuCauSuaDon(LocalDateTime.now());
        donHang.setTrangThaiYeuCauSuaDon("CHO_XU_LY");

        DonHang updatedDonHang = donHangRepository.save(donHang);

        // FIX CHÍNH của DF_ST06: đẩy thông báo realtime cho kênh nhân viên/admin,
        // đồng bộ với các luồng notify khác đã có (đơn mới, hủy đơn...) để nhân
        // viên biết ngay và không còn bị bỏ sót yêu cầu sửa đơn của khách.
        notificationService.notifyNewOrderToAdmins(
                "✏️ Khách hàng vừa gửi yêu cầu sửa thông tin đơn HD-" + donHangId +
                        ". Vui lòng vào chi tiết đơn để xem và duyệt yêu cầu.");

        return mapToResponseDto(updatedDonHang, false);
    }

    public Map<String, Object> get3DCakeDesign(Long orderId) {
        DonHang donHang = donHangRepository.findById(orderId)
                .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen("Không tìm thấy đơn hàng có ID: " + orderId));

        String emailUserHienTai = SecurityContextHolder.getContext().getAuthentication().getName();
        NguoiDung userHienTai = nguoiDungRepository.findByEmail(emailUserHienTai)
                .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen("Lỗi xác thực: Không tìm thấy tài khoản!"));

        if ("KHACH_HANG".equals(userHienTai.getQuyen())) {
            if (donHang.getKhachHang() == null || !donHang.getKhachHang().getId().equals(userHienTai.getId())) {
                throw new NgoaiLeCamTruyCap("Bạn không có quyền xem thiết kế của đơn hàng này!");
            }
        }

        String designJson = donHang.getThietKeBanhJson();
        if (designJson == null || designJson.trim().isEmpty()) {
            throw new NgoaiLeNghiepVu("Đơn hàng này không có dữ liệu thiết kế 3D");
        }

        try {
            return objectMapper.readValue(designJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new NgoaiLeNghiepVu("Lỗi khi parse dữ liệu 3D Design: " + e.getMessage());
        }
    }

    @Transactional
    public void updatePaymentStatus(Long orderId, BigDecimal soTienNhanDuoc) {
        DonHang donHang = donHangRepository.findById(orderId)
                .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen("Không tìm thấy đơn hàng: " + orderId));

        if (donHang.getTrangThai() != TrangThaiDonHang.CHO_XAC_NHAN) {
            log.info("Webhook SePay: đơn DH{} đã được xử lý thanh toán trước đó (trạng thái hiện tại: {}), bỏ qua.",
                    orderId, donHang.getTrangThai());
            return;
        }

        BigDecimal tongTien = donHang.getTongTien() != null ? donHang.getTongTien() : BigDecimal.ZERO;

        if (soTienNhanDuoc == null || soTienNhanDuoc.compareTo(tongTien) < 0) {
            log.warn("⚠️ Webhook SePay: đơn DH{} cần {} nhưng chỉ nhận được {} -> KHÔNG đánh dấu đã thanh toán, giữ nguyên trạng thái chờ.",
                    orderId, tongTien, soTienNhanDuoc);
            throw new NgoaiLeNghiepVu(
                    "Số tiền chuyển khoản (" + soTienNhanDuoc + ") nhỏ hơn tổng tiền đơn hàng (" + tongTien + ")");
        }

        thanhToanRepository.findByDonHang(donHang).ifPresent(tt -> {
            tt.setTrangThai("THANH_CONG");
            tt.setSoTien(soTienNhanDuoc);
            tt.setThoiDiemThanhToan(LocalDateTime.now());
            thanhToanRepository.save(tt);
        });

        donHang.setTrangThai(TrangThaiDonHang.DA_XAC_NHAN);
        donHangRepository.save(donHang);
        // LƯU Ý NGHIỆP VỤ: KHÔNG trừ tồn kho ở đây nữa. Trước đây trừ kho ngay khi
        // thanh toán/xác nhận thành công, nhưng nghiệp vụ đúng là: chỉ trừ tồn kho
        // khi đơn được cửa hàng chuyển sang trạng thái SAN_SANG (sẵn sàng giao) —
        // xem processOrder() bên dưới, nơi trừ kho thực sự diễn ra.
        notificationService.notifyNewOrderToAdmins(
                "✅ Đơn hàng DH" + orderId + " đã thanh toán qua SePay, chuyển sang DA_XAC_NHAN!");
    }

    private DatHangDto.Response mapToResponseDto(DonHang donHang, boolean withInternalNote) {
        DatHangDto.Response dto = new DatHangDto.Response();
        dto.setId(donHang.getId());
        dto.setMaDonHang("HD-" + donHang.getId());
        dto.setDiaChiGiaoHang(donHang.getDiaChiGiao());

        if (donHang.getKhachHang() != null) {
            dto.setSoDienThoai(donHang.getKhachHang().getSoDienThoai());
            dto.setEmailNguoiDung(donHang.getKhachHang().getEmail());
        }

        if (donHang.getNgayGiaoDuKien() != null) {
            dto.setNgayGiaoHang(donHang.getNgayGiaoDuKien().toLocalDate());
        }

        dto.setNgayTao(donHang.getNgayTao());

        if (donHang.getTongTien() != null) {
            dto.setTongTien(donHang.getTongTien().doubleValue());
        }

        dto.setSoTienPhuThu(donHang.getSoTienPhuThu() != null
                ? donHang.getSoTienPhuThu().doubleValue() : 0.0);

        dto.setTrangThai(donHang.getTrangThai().name());
        dto.setGhiChu(donHang.getGhiChu());

        if (withInternalNote) {
            dto.setGhiChuNoiBo(donHang.getGhiChuNoiBo());
        }

        if (donHang.getNhanVien() != null) {
            dto.setTenNhanVienPhuTrach(donHang.getNhanVien().getHoTen());
        }
        dto.setLyDoHuy(donHang.getLyDoHuy());
        dto.setCoThietKe3D(donHang.getThietKeBanhJson() != null &&
                !donHang.getThietKeBanhJson().trim().isEmpty());
        dto.setTrangThaiYeuCauSuaDon(donHang.getTrangThaiYeuCauSuaDon());

        if (donHang.getChiTietDonHangs() != null) {
            List<DatHangDto.OrderItemResponse> itemDtos = donHang.getChiTietDonHangs().stream()
                    .map(item -> {
                        DatHangDto.OrderItemResponse itemDto = new DatHangDto.OrderItemResponse();
                        itemDto.setSanPhamId(item.getSanPham().getId());
                        itemDto.setTenSanPham(item.getSanPham().getTenSanPham());
                        itemDto.setSoLuong(item.getSoLuong());
                        itemDto.setGiaBan(item.getDonGiaTaiThoiDiem() != null
                                ? item.getDonGiaTaiThoiDiem().doubleValue() : 0.0);
                        itemDto.setThietKeBanhJson(item.getThietKeBanhJson());
                        return itemDto;
                    }).collect(Collectors.toList());
            dto.setItems(itemDtos);

            if (donHang.getMaGiamGia() != null) {
                BigDecimal tongTienHangGoc = donHang.getChiTietDonHangs().stream()
                        .map(item -> (item.getDonGiaTaiThoiDiem() != null ? item.getDonGiaTaiThoiDiem() : BigDecimal.ZERO)
                                .multiply(BigDecimal.valueOf(item.getSoLuong())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                dto.setMaGiamGiaCode(donHang.getMaGiamGia().getMaCode());
                dto.setSoTienGiam(tinhSoTienGiam(donHang.getMaGiamGia(), tongTienHangGoc).doubleValue());
            } else if (donHang.getVoucherKhachHang() != null) {
                BigDecimal tongTienHangGoc = donHang.getChiTietDonHangs().stream()
                        .map(item -> (item.getDonGiaTaiThoiDiem() != null ? item.getDonGiaTaiThoiDiem() : BigDecimal.ZERO)
                                .multiply(BigDecimal.valueOf(item.getSoLuong())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                dto.setTenVoucherKhachHang(donHang.getVoucherKhachHang().getTenVoucher());
                dto.setSoTienGiam(tinhSoTienGiamVoucher(donHang.getVoucherKhachHang(), tongTienHangGoc).doubleValue());
            }
        }
        return dto;
    }

    private void kiemTraMaGiamGiaHopLe(MaGiamGia maGiamGia, BigDecimal tongTienHang) {
        if (!Boolean.TRUE.equals(maGiamGia.getHoatDong())) {
            throw new NgoaiLeNghiepVu("Mã giảm giá \"" + maGiamGia.getMaCode() + "\" hiện không còn hoạt động, vui lòng gỡ mã ở giỏ hàng!");
        }
        if (maGiamGia.getNgayHetHan() != null && maGiamGia.getNgayHetHan().isBefore(LocalDateTime.now())) {
            throw new NgoaiLeNghiepVu("Mã giảm giá \"" + maGiamGia.getMaCode() + "\" đã hết hạn, vui lòng gỡ mã ở giỏ hàng!");
        }
        if (maGiamGia.getSoLuotToiDa() != null
                && maGiamGia.getSoLuotDaDung() != null
                && maGiamGia.getSoLuotDaDung() >= maGiamGia.getSoLuotToiDa()) {
            throw new NgoaiLeNghiepVu("Mã giảm giá \"" + maGiamGia.getMaCode() + "\" đã hết lượt sử dụng, vui lòng gỡ mã ở giỏ hàng!");
        }
        if (maGiamGia.getDonHangToiThieu() != null
                && tongTienHang.compareTo(maGiamGia.getDonHangToiThieu()) < 0) {
            throw new NgoaiLeNghiepVu(
                    "Đơn hàng chưa đạt giá trị tối thiểu " + maGiamGia.getDonHangToiThieu()
                            + " để áp dụng mã \"" + maGiamGia.getMaCode() + "\", vui lòng gỡ mã ở giỏ hàng!");
        }
    }

    private BigDecimal tinhSoTienGiam(MaGiamGia maGiamGia, BigDecimal tongTienHang) {
        BigDecimal soTienGiam;
        if ("PHAN_TRAM".equals(maGiamGia.getLoaiGiamGia())) {
            soTienGiam = tongTienHang.multiply(maGiamGia.getGiaTriGiam())
                    .divide(BigDecimal.valueOf(100));
        } else {
            soTienGiam = maGiamGia.getGiaTriGiam();
        }
        return soTienGiam.min(tongTienHang);
    }

    private void kiemTraVoucherKhachHangHopLe(VoucherKhachHang voucher, BigDecimal tongTienHang) {
        if (!"CHUA_SU_DUNG".equals(voucher.getTrangThai())) {
            throw new NgoaiLeNghiepVu("Voucher đã được sử dụng hoặc không còn hiệu lực, vui lòng gỡ ở giỏ hàng!");
        }
        if (voucher.getNgayHetHan() != null && voucher.getNgayHetHan().isBefore(LocalDateTime.now())) {
            throw new NgoaiLeNghiepVu("Voucher đã hết hạn, vui lòng gỡ ở giỏ hàng!");
        }
        if (voucher.getDonHangToiThieu() != null
                && tongTienHang.compareTo(voucher.getDonHangToiThieu()) < 0) {
            throw new NgoaiLeNghiepVu(
                    "Đơn hàng chưa đạt giá trị tối thiểu " + voucher.getDonHangToiThieu()
                            + " để áp dụng voucher này, vui lòng gỡ ở giỏ hàng!");
        }
    }

    private BigDecimal tinhSoTienGiamVoucher(VoucherKhachHang voucher, BigDecimal tongTienHang) {
        BigDecimal soTienGiam;
        if ("PHAN_TRAM".equals(voucher.getLoaiGiam())) {
            soTienGiam = tongTienHang.multiply(voucher.getGiaTriGiam())
                    .divide(BigDecimal.valueOf(100));
        } else {
            soTienGiam = voucher.getGiaTriGiam();
        }
        return soTienGiam.min(tongTienHang);
    }
}