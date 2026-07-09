package com.poly.cake.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.cake.dto.OrderDto;
import com.poly.cake.dto.OrderProcessDto;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.ChiTietDonHang;
import com.poly.cake.entity.GioHang;
import com.poly.cake.entity.MaGiamGia;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.SanPham;
import com.poly.cake.entity.ThanhToan;
import com.poly.cake.entity.TrangThaiDonHang; // Đã thêm import Enum
import com.poly.cake.entity.VoucherKhachHang;
import com.poly.cake.exception.BusinessException;
import com.poly.cake.exception.ForbiddenException;
import com.poly.cake.exception.ResourceNotFoundException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
public class OrderService {

    private final DonHangRepository donHangRepository;

    private final ChiTietDonHangRepository chiTietDonHangRepository;

    private final NguoiDungRepository nguoiDungRepository;

    private final SanPhamRepository sanPhamRepository;

    private final NotificationService notificationService;

    private final ThanhToanRepository thanhToanRepository;

    // Dùng để trừ tồn kho + tự động cảnh báo tồn kho thấp ngay khi thanh toán thành công
    private final InventoryService inventoryService;

    // Giỏ hàng & mã giảm giá — dùng để tự động mang mã đã áp ở giỏ hàng sang lúc checkout
    private final GioHangRepository gioHangRepository;

    private final MaGiamGiaRepository maGiamGiaRepository;

    private final VoucherKhachHangRepository voucherKhachHangRepository;

    // T055 – Validator thiết kế bánh 3D
    private final CakeDesignValidator cakeDesignValidator;

    // T072 – Gửi email xác nhận đơn hàng (kèm PDF hóa đơn) + email hủy đơn/hoàn tiền
    private final EmailService emailService;
    private final InvoicePdfService invoicePdfService;

    // T103 – Discord webhook: thông báo đơn mới / đơn hủy cho Admin
    private final DiscordWebhookService discordWebhookService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. TẠO ĐƠN HÀNG (CHECKOUT)
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public OrderDto.Response createOrder(OrderDto.Request request, String emailNguoiDung) {

        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailNguoiDung)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản!"));

        if (request.getNgayGiaoHang() == null || request.getNgayGiaoHang().isBefore(LocalDate.now())) {
            throw new BusinessException("Ngày giao hàng không hợp lệ! Phải chọn từ ngày hôm nay trở đi.");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Giỏ hàng trống! Không thể đặt hàng.");
        }

        Map<String, Object> designMap = cakeDesignValidator.validateAndParse(request.getCakeDesignJson());

        double tongTienHang = request.getItems().stream()
                .mapToDouble(item -> item.getDonGia() * item.getSoLuong())
                .sum();

        // Mang mã giảm giá đã áp ở giỏ hàng (nếu có) sang đơn checkout này
        GioHang gioHang = gioHangRepository.findByKhachHang(khachHang).orElse(null);
        MaGiamGia maGiamGiaApDung = gioHang != null ? gioHang.getMaGiamGia() : null;
        // Voucher cá nhân (đổi bằng điểm) đã áp ở giỏ hàng — chỉ có giá trị khi
        // không đồng thời có mã giảm giá (2 loại ưu đãi loại trừ nhau ở giỏ hàng)
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
        BigDecimal tongTienThanhToan = BigDecimal.valueOf(tongTienHang)
                .subtract(soTienGiam)
                .add(BigDecimal.valueOf(phiShip));

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
        donHang.setGhiChu(ghiChuCuoiCung);
        // Thay thế String bằng Enum
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
            // Không có cakeDesignJson theo schema cũ (khung/tang/trang_tri) - đây là luồng
            // thực tế của CakeBuilder3D (Design.vue): mỗi item trong giỏ tự mang theo JSON
            // thiết kế thô của riêng nó (shape/size/tierCount/frostingColor/accessories...).
            // Lấy item đầu tiên có thiết kế để lưu ở cấp đơn hàng, cho trang bếp
            // (BakeryOrders.vue -> GET /api/v1/orders/{id}/design) vẫn xem được bình
            // thường - CakeDesignViewer3D.vue đã tự nhận diện đúng định dạng thô này.
            request.getItems().stream()
                    .map(OrderDto.OrderItemRequest::getThietKeBanhJson)
                    .filter(json -> json != null && !json.isBlank())
                    .findFirst()
                    .ifPresent(donHang::setThietKeBanhJson);
        }

        DonHang savedDonHang = donHangRepository.save(donHang);

        List<ChiTietDonHang> chiTietList = request.getItems().stream().map(itemDto -> {
            SanPham sanPham = sanPhamRepository.findById(itemDto.getSanPhamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại!"));
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

        // Ghi nhận đã dùng mã giảm giá + gỡ khỏi giỏ hàng để lần đặt sau không bị áp nhầm lại
        if (maGiamGiaApDung != null) {
            maGiamGiaApDung.setSoLuotDaDung(
                    (maGiamGiaApDung.getSoLuotDaDung() == null ? 0 : maGiamGiaApDung.getSoLuotDaDung()) + 1);
            maGiamGiaRepository.save(maGiamGiaApDung);

            gioHang.setMaGiamGia(null);
            gioHangRepository.save(gioHang);
        } else if (voucherApDung != null) {
            // Ghi nhận voucher cá nhân đã được dùng + gỡ khỏi giỏ hàng
            voucherApDung.setTrangThai("DA_SU_DUNG");
            voucherApDung.setNgaySuDung(LocalDateTime.now());
            voucherKhachHangRepository.save(voucherApDung);

            // Nếu voucher này được đổi ra từ 1 mã giảm giá gốc (đổi điểm), cộng dồn lượt
            // sử dụng ngược về mã gốc để trang quản lý voucher (đọc bảng ma_giam_gia)
            // thống kê đúng — trước đây bước này bị thiếu nên admin không thấy được lượt
            // dùng của các voucher đổi bằng điểm.
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

        // ═══════════════════════════════════════════════════════════════
        // T072 – Đặt hàng thành công → tự động gửi email xác nhận kèm PDF
        // hóa đơn cho khách. Đây là hành động "best-effort" đi kèm: nếu gửi
        // email thất bại (SMTP lỗi, mạng chập chờn...) thì KHÔNG được phép
        // làm rollback cả giao dịch đặt hàng chính, chỉ log lại lỗi để
        // theo dõi/gửi lại thủ công sau.
        // ═══════════════════════════════════════════════════════════════
        // T072 – Gửi email xác nhận
        try {
            byte[] pdfHoaDon = invoicePdfService.generateInvoicePdf(savedDonHang);
            emailService.sendOrderConfirmationEmail(
                    khachHang.getEmail(), khachHang.getHoTen(), "HD-" + savedDonHang.getId(), pdfHoaDon);
        } catch (Exception e) {
            log.error("Gửi email xác nhận đơn hàng HD-{} thất bại: {}", savedDonHang.getId(), e.getMessage(), e);
        }

        // T103 – Discord: thông báo không đồng bộ đến Admin (best-effort, không rollback nếu lỗi)
        try {
            discordWebhookService.sendNewOrderNotification(
                    savedDonHang.getId(),
                    khachHang.getHoTen(),
                    tongTienThanhToan.doubleValue());
        } catch (Exception e) {
            log.warn("Gửi Discord thông báo đơn mới HD-{} thất bại: {}", savedDonHang.getId(), e.getMessage());
        }

        return mapToResponseDto(savedDonHang);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. XEM LỊCH SỬ ĐƠN HÀNG
    // ═══════════════════════════════════════════════════════════════════════════
    public List<OrderDto.Response> getOrdersByUser(String email) {
        NguoiDung khachHang = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại."));
        // [T105 - Fix N+1] Dùng query có JOIN FETCH thay cho findByKhachHangOrderByNgayTaoDesc()
        // → tải toàn bộ chi tiết + sản phẩm trong 1 câu SQL duy nhất
        return donHangRepository.findByKhachHangWithDetails(khachHang)
                .stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3. XEM TOÀN BỘ ĐƠN HÀNG
    // ═══════════════════════════════════════════════════════════════════════════
    public List<OrderDto.Response> getAllOrders() {
        return donHangRepository.findAll().stream()
                .map(this::mapToResponseDto).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 4. XEM CHI TIẾT ĐƠN HÀNG BY ID
    // ═══════════════════════════════════════════════════════════════════════════
    public OrderDto.Response getOrderById(Long id, String email, String role) {
        // [T105 - Fix N+1] Dùng findByIdWithDetails() thay cho findById()
        // → tải sẵn chi tiết + sản phẩm + khách hàng + nhân viên trong 1 query
        DonHang donHang = donHangRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + id));

        if ("ROLE_KHACH_HANG".equals(role) || "KHACH_HANG".equals(role)) {
            if (donHang.getKhachHang() == null || !donHang.getKhachHang().getEmail().equals(email)) {
                throw new ForbiddenException("Bạn không có quyền xem đơn hàng này!");
            }
        }

        return mapToResponseDto(donHang);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 4.1. T072 – TẢI LẠI PDF HÓA ĐƠN CỦA 1 ĐƠN HÀNG (bổ trợ cho trang Lịch sử đơn)
    // Dùng chung điều kiện phân quyền với getOrderById: khách chỉ xem được đơn của
    // chính mình, Admin/NhanVien xem được tất cả.
    // ═══════════════════════════════════════════════════════════════════════════
    public byte[] getInvoicePdf(Long id, String email, String role) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + id));

        if ("ROLE_KHACH_HANG".equals(role) || "KHACH_HANG".equals(role)) {
            if (donHang.getKhachHang() == null || !donHang.getKhachHang().getEmail().equals(email)) {
                throw new ForbiddenException("Bạn không có quyền xem hóa đơn của đơn hàng này!");
            }
        }

        return invoicePdfService.generateInvoicePdf(donHang);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 5. XỬ LÝ ĐƠN HÀNG (Admin / Nhân viên)
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public OrderDto.Response processOrder(Long id, OrderProcessDto request, String emailNhanVien) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại."));

        NguoiDung nhanVien = nguoiDungRepository.findByEmail(emailNhanVien)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin nhân viên xử lý!"));

        donHang.setNhanVien(nhanVien);

        // Chuyển String thành Enum an toàn
        TrangThaiDonHang trangThaiMoi;
        try {
            trangThaiMoi = TrangThaiDonHang.valueOf(request.getTrangThai().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Trạng thái đơn hàng không hợp lệ!");
        }

        TrangThaiDonHang trangThaiHienTai = donHang.getTrangThai();

        if (trangThaiHienTai == TrangThaiDonHang.HOAN_THANH || trangThaiHienTai == TrangThaiDonHang.DA_HUY) {
            throw new BusinessException("Đơn hàng đã chốt (Giao/Hủy/Hoàn tiền) thì không thể thay đổi trạng thái được nữa!");
        }

        // Cập nhật DANG_CHUAN_BI thành DANG_LAM cho khớp Enum
        if (trangThaiHienTai == TrangThaiDonHang.DANG_GIAO &&
                (trangThaiMoi == TrangThaiDonHang.CHO_XAC_NHAN || trangThaiMoi == TrangThaiDonHang.DANG_LAM)) {
            throw new BusinessException("Đơn hàng đang giao, không thể lùi trạng thái!");
        }

        donHang.setTrangThai(trangThaiMoi);

        if (trangThaiMoi == TrangThaiDonHang.DA_HUY) {
            if (request.getLyDoHuy() == null || request.getLyDoHuy().trim().isEmpty()) {
                throw new BusinessException("Bắt buộc phải nhập lý do khi hủy đơn hàng!");
            }
            donHang.setLyDoHuy(request.getLyDoHuy());
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

        return mapToResponseDto(updatedDonHang);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 6. HỦY ĐƠN HÀNG (Khách tự hủy)
    // T072: Kiểm tra điều kiện hủy + hoàn tiền cọc/thanh toán nếu có
    // ═══════════════════════════════════════════════════════════════════════════
    // Các trạng thái mà khách còn được QUYỀN tự hủy đơn: đơn còn đang chờ xác nhận
    // hoặc đã xác nhận nhưng bếp CHƯA bắt tay vào làm. Một khi đơn đã DANG_LAM trở
    // đi (bếp đã dùng nguyên liệu/nhân công) thì không cho tự hủy nữa, khách phải
    // liên hệ trực tiếp cửa hàng/admin để xử lý.
    private static final EnumSet<TrangThaiDonHang> TRANG_THAI_CON_DUOC_TU_HUY = EnumSet.of(
            TrangThaiDonHang.CHO_XAC_NHAN, TrangThaiDonHang.DA_XAC_NHAN);

    @Transactional
    public OrderDto.Response cancelOrder(Long id, String emailNguoiDung) {
        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailNguoiDung)
                .orElseThrow(() -> new BusinessException("Tài khoản không hợp lệ."));

        DonHang donHang = donHangRepository.findByIdAndKhachHang(id, khachHang)
                .orElseThrow(() -> new ForbiddenException(
                        "Đơn hàng không thuộc quyền sở hữu của bạn hoặc không tồn tại."));

        TrangThaiDonHang trangThaiHienTai = donHang.getTrangThai();

        // ── BƯỚC 1: Kiểm tra điều kiện hủy ────────────────────────────────
        if (!TRANG_THAI_CON_DUOC_TU_HUY.contains(trangThaiHienTai)) {
            throw new BusinessException(
                    "Đơn hàng đang ở trạng thái \"" + trangThaiHienTai.name() +
                            "\", đã được cửa hàng xử lý nên không thể tự hủy vào lúc này. " +
                            "Vui lòng liên hệ cửa hàng nếu cần hỗ trợ thêm!");
        }

        // ── BƯỚC 2: Kiểm tra xem đơn đã thanh toán/đặt cọc trước đó chưa ──
        // Nếu có bản ghi ThanhToan với trạng thái THANH_CONG (khách đã chuyển khoản
        // qua SePay hoặc đã đặt cọc) thì bắt buộc phải hoàn tiền khi hủy.
        Optional<ThanhToan> thanhToanOpt = thanhToanRepository.findByDonHang(donHang);
        boolean daThanhToan = thanhToanOpt.isPresent() && "THANH_CONG".equals(thanhToanOpt.get().getTrangThai());
        BigDecimal soTienHoan = null;

        if (daThanhToan) {
            ThanhToan thanhToan = thanhToanOpt.get();
            soTienHoan = thanhToan.getSoTien();

            // Đánh dấu giao dịch đã được hoàn tiền (hoàn tiền thủ công qua chuyển khoản
            // ngược - hệ thống chưa tích hợp cổng hoàn tiền tự động của SePay/VNPay).
            thanhToan.setTrangThai("DA_HOAN_TIEN");
            thanhToanRepository.save(thanhToan);

            // Đơn đã thanh toán thành công nghĩa là lúc trước đã bị trừ tồn kho
            // (xem updatePaymentStatus() -> InventoryService.truTonKhoTheoDonHang),
            // nên khi hủy + hoàn tiền phải hoàn trả lại đúng số lượng đã trừ.
            for (ChiTietDonHang ct : donHang.getChiTietDonHangs()) {
                sanPhamRepository.congLaiSoLuongTon(ct.getSanPham().getId(), ct.getSoLuong());
            }

            // Đơn đã có giao dịch tiền -> dùng trạng thái chuyên biệt DA_HOAN_TIEN
            // (khác DA_HUY thường) để admin dễ lọc/đối soát các đơn cần hoàn tiền.
            donHang.setTrangThai(TrangThaiDonHang.DA_HOAN_TIEN);
            donHang.setLyDoHuy("Khách hàng tự hủy trên web - đã hoàn tiền "
                    + soTienHoan + "đ vào " + LocalDateTime.now());
        } else {
            // Chưa phát sinh thanh toán (COD/chưa chuyển khoản) -> hủy bình thường,
            // không cần hoàn tiền, cũng không cần hoàn kho (vì chưa từng bị trừ).
            donHang.setTrangThai(TrangThaiDonHang.DA_HUY);
            donHang.setLyDoHuy("Khách hàng tự hủy trên web");
        }

        DonHang updatedDonHang = donHangRepository.save(donHang);

        // ── BƯỚC 3: Thông báo real-time cho khách + admin ─────────────────
        String thongBaoKhach = "Đơn hàng HD-" + id + " của bạn đã được hủy thành công."
                + (daThanhToan ? " Số tiền " + soTienHoan + "đ sẽ được hoàn lại trong 1-3 ngày làm việc." : "");
        notificationService.notifyOrderStatusToUser(emailNguoiDung, thongBaoKhach);
        notificationService.notifyNewOrderToAdmins(
                "❌ Khách hàng đã tự hủy đơn HD-" + id + (daThanhToan ? " (CẦN ĐỐI SOÁT HOÀN TIỀN)" : ""));

        // BƯỚC 4: Gửi email xác nhận hủy đơn (best-effort)
        try {
            emailService.sendOrderCancellationEmail(
                    khachHang.getEmail(), khachHang.getHoTen(), "HD-" + id,
                    updatedDonHang.getLyDoHuy(), soTienHoan);
        } catch (Exception e) {
            log.error("Gửi email hủy đơn HD-{} thất bại: {}", id, e.getMessage(), e);
        }

        // T103 – Discord: thông báo đơn bị hủy (best-effort)
        try {
            discordWebhookService.sendOrderCancelledNotification(
                    id,
                    khachHang.getHoTen(),
                    updatedDonHang.getLyDoHuy(),
                    daThanhToan);
        } catch (Exception e) {
            log.warn("Gửi Discord thông báo hủy đơn HD-{} thất bại: {}", id, e.getMessage());
        }

        return mapToResponseDto(updatedDonHang);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 7. LẤY DỮ LIỆU THIẾT KẾ 3D
    // ═══════════════════════════════════════════════════════════════════════════
    public Map<String, Object> get3DCakeDesign(Long orderId) {
        DonHang donHang = donHangRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng có ID: " + orderId));

        String emailUserHienTai = SecurityContextHolder.getContext().getAuthentication().getName();
        NguoiDung userHienTai = nguoiDungRepository.findByEmail(emailUserHienTai)
                .orElseThrow(() -> new ResourceNotFoundException("Lỗi xác thực: Không tìm thấy tài khoản!"));

        if ("KHACH_HANG".equals(userHienTai.getQuyen())) {
            if (donHang.getKhachHang() == null || !donHang.getKhachHang().getId().equals(userHienTai.getId())) {
                throw new ForbiddenException("Bạn không có quyền xem thiết kế của đơn hàng này!");
            }
        }

        String designJson = donHang.getThietKeBanhJson();
        if (designJson == null || designJson.trim().isEmpty()) {
            throw new BusinessException("Đơn hàng này không có dữ liệu thiết kế 3D");
        }

        try {
            return objectMapper.readValue(designJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new BusinessException("Lỗi khi parse dữ liệu 3D Design: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEPAY WEBHOOK: Cập nhật trạng thái sau khi thanh toán
    // soTienNhanDuoc: số tiền thực tế ngân hàng báo về trong webhook, dùng để
    // đối chiếu với tổng tiền đơn hàng, tránh đánh dấu "đã thanh toán đủ"
    // cho một giao dịch chuyển khoản thiếu tiền nhưng đúng nội dung DH.
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public void updatePaymentStatus(Long orderId, BigDecimal soTienNhanDuoc) {
        DonHang donHang = donHangRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        // Chống xử lý trùng lặp: SePay có thể gọi lại webhook (retry) cho cùng 1 giao
        // dịch. Nếu đơn đã được xác nhận thanh toán trước đó rồi thì bỏ qua, KHÔNG
        // trừ tồn kho thêm lần nữa (nếu không sẽ bị trừ kho gấp đôi).
        if (donHang.getTrangThai() != TrangThaiDonHang.CHO_XAC_NHAN) {
            log.info("Webhook SePay: đơn DH{} đã được xử lý thanh toán trước đó (trạng thái hiện tại: {}), bỏ qua.",
                    orderId, donHang.getTrangThai());
            return;
        }

        BigDecimal tongTien = donHang.getTongTien() != null ? donHang.getTongTien() : BigDecimal.ZERO;

        // Đối chiếu số tiền: nếu chuyển thiếu so với tổng tiền đơn hàng thì
        // KHÔNG đánh dấu thanh toán thành công, chỉ ghi nhận cảnh báo để admin xử lý thủ công.
        if (soTienNhanDuoc == null || soTienNhanDuoc.compareTo(tongTien) < 0) {
            log.warn("⚠️ Webhook SePay: đơn DH{} cần {} nhưng chỉ nhận được {} -> KHÔNG đánh dấu đã thanh toán, giữ nguyên trạng thái chờ.",
                    orderId, tongTien, soTienNhanDuoc);
            throw new BusinessException(
                    "Số tiền chuyển khoản (" + soTienNhanDuoc + ") nhỏ hơn tổng tiền đơn hàng (" + tongTien + ")");
        }

        thanhToanRepository.findByDonHang(donHang).ifPresent(tt -> {
            tt.setTrangThai("THANH_CONG");
            tt.setSoTien(soTienNhanDuoc);
            tt.setThoiDiemThanhToan(LocalDateTime.now());
            thanhToanRepository.save(tt);
        });

        // Cập nhật trạng thái bằng Enum
        donHang.setTrangThai(TrangThaiDonHang.DA_XAC_NHAN);
        donHangRepository.save(donHang);

        // Thanh toán THÀNH CÔNG -> trừ tồn kho cho toàn bộ sản phẩm trong đơn hàng.
        // InventoryService tự lo việc trừ an toàn (không âm kho) + tự gửi cảnh báo
        // tồn kho thấp (ngưỡng mặc định = 10) hoặc cảnh báo bán vượt tồn kho nếu cần.
        inventoryService.truTonKhoTheoDonHang(donHang);

        notificationService.notifyNewOrderToAdmins(
                "✅ Đơn hàng DH" + orderId + " đã thanh toán qua SePay, chuyển sang DA_XAC_NHAN!");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HÀM PHỤ TRỢ – Entity → DTO
    // ═══════════════════════════════════════════════════════════════════════════
    private OrderDto.Response mapToResponseDto(DonHang donHang) {
        OrderDto.Response dto = new OrderDto.Response();
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

        // Lấy tên chuỗi từ Enum để gán vào DTO
        dto.setTrangThai(donHang.getTrangThai().name());
        dto.setGhiChu(donHang.getGhiChu());

        if (donHang.getNhanVien() != null) {
            dto.setTenNhanVienPhuTrach(donHang.getNhanVien().getHoTen());
        }
        dto.setLyDoHuy(donHang.getLyDoHuy());

        dto.setCoThietKe3D(donHang.getThietKeBanhJson() != null &&
                !donHang.getThietKeBanhJson().trim().isEmpty());

        if (donHang.getChiTietDonHangs() != null) {
            List<OrderDto.OrderItemResponse> itemDtos = donHang.getChiTietDonHangs().stream()
                    .map(item -> {
                        OrderDto.OrderItemResponse itemDto = new OrderDto.OrderItemResponse();
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

    // ─── HELPER: Kiểm tra mã giảm giá còn hợp lệ để áp dụng lúc checkout không ──
    private void kiemTraMaGiamGiaHopLe(MaGiamGia maGiamGia, BigDecimal tongTienHang) {
        if (!Boolean.TRUE.equals(maGiamGia.getHoatDong())) {
            throw new BusinessException("Mã giảm giá \"" + maGiamGia.getMaCode() + "\" hiện không còn hoạt động, vui lòng gỡ mã ở giỏ hàng!");
        }
        if (maGiamGia.getNgayHetHan() != null && maGiamGia.getNgayHetHan().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Mã giảm giá \"" + maGiamGia.getMaCode() + "\" đã hết hạn, vui lòng gỡ mã ở giỏ hàng!");
        }
        if (maGiamGia.getSoLuotToiDa() != null
                && maGiamGia.getSoLuotDaDung() != null
                && maGiamGia.getSoLuotDaDung() >= maGiamGia.getSoLuotToiDa()) {
            throw new BusinessException("Mã giảm giá \"" + maGiamGia.getMaCode() + "\" đã hết lượt sử dụng, vui lòng gỡ mã ở giỏ hàng!");
        }
        if (maGiamGia.getDonHangToiThieu() != null
                && tongTienHang.compareTo(maGiamGia.getDonHangToiThieu()) < 0) {
            throw new BusinessException(
                    "Đơn hàng chưa đạt giá trị tối thiểu " + maGiamGia.getDonHangToiThieu()
                            + " để áp dụng mã \"" + maGiamGia.getMaCode() + "\", vui lòng gỡ mã ở giỏ hàng!");
        }
    }

    // ─── HELPER: Tính số tiền được giảm theo loại mã ────────────────────────
    private BigDecimal tinhSoTienGiam(MaGiamGia maGiamGia, BigDecimal tongTienHang) {
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

    // ─── HELPER: Kiểm tra voucher cá nhân còn hợp lệ để áp dụng lúc checkout không ──
    private void kiemTraVoucherKhachHangHopLe(VoucherKhachHang voucher, BigDecimal tongTienHang) {
        if (!"CHUA_SU_DUNG".equals(voucher.getTrangThai())) {
            throw new BusinessException("Voucher đã được sử dụng hoặc không còn hiệu lực, vui lòng gỡ ở giỏ hàng!");
        }
        if (voucher.getNgayHetHan() != null && voucher.getNgayHetHan().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Voucher đã hết hạn, vui lòng gỡ ở giỏ hàng!");
        }
        if (voucher.getDonHangToiThieu() != null
                && tongTienHang.compareTo(voucher.getDonHangToiThieu()) < 0) {
            throw new BusinessException(
                    "Đơn hàng chưa đạt giá trị tối thiểu " + voucher.getDonHangToiThieu()
                            + " để áp dụng voucher này, vui lòng gỡ ở giỏ hàng!");
        }
    }

    // ─── HELPER: Tính số tiền được giảm theo voucher cá nhân ────────────────
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