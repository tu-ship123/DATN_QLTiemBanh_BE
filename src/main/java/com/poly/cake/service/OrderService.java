package com.poly.cake.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.cake.dto.OrderDto;
import com.poly.cake.dto.OrderProcessDto;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.ChiTietDonHang;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.SanPham;
import com.poly.cake.exception.BusinessException;
import com.poly.cake.repository.DonHangRepository;
import com.poly.cake.repository.ChiTietDonHangRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.SanPhamRepository;
import com.poly.cake.repository.ThanhToanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ThanhToanRepository thanhToanRepository;

    // T055 – Validator thiết kế bánh 3D
    @Autowired
    private CakeDesignValidator cakeDesignValidator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. TẠO ĐƠN HÀNG (CHECKOUT)
    //    T055: Nếu request có cakeDesignJson → validate → lưu thietKeBanhJson
    //          và tự động ghi chú kích thước vào ghi_chu để thợ làm bánh biết
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public OrderDto.Response createOrder(OrderDto.Request request, String emailNguoiDung) {

        // ── Kiểm tra tài khoản ────────────────────────────────────────────────
        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailNguoiDung)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản!"));

        // ── Kiểm tra ngày giao ────────────────────────────────────────────────
        if (request.getNgayGiaoHang() == null || request.getNgayGiaoHang().isBefore(LocalDate.now())) {
            throw new RuntimeException("Ngày giao hàng không hợp lệ! Phải chọn từ ngày hôm nay trở đi.");
        }

        // ── Kiểm tra giỏ hàng ────────────────────────────────────────────────
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống! Không thể đặt hàng.");
        }

        // ── T055: Validate & parse dữ liệu thiết kế 3D ───────────────────────
        // validateAndParse() sẽ:
        //   • Trả về null  nếu cakeDesignJson == null (đơn bình thường)
        //   • Throw BusinessException nếu JSON sai cấu trúc hoặc thiếu kích thước
        //   • Trả về Map đã parse nếu hợp lệ
        Map<String, Object> designMap = cakeDesignValidator.validateAndParse(request.getCakeDesignJson());

        // ── Tính tiền ─────────────────────────────────────────────────────────
        double tongTienHang = request.getItems().stream()
                .mapToDouble(item -> item.getDonGia() * item.getSoLuong())
                .sum();
        double phiShip = (tongTienHang >= 500_000) ? 0.0 : 30_000.0;
        BigDecimal tongTienThanhToan = BigDecimal.valueOf(tongTienHang + phiShip);

        // ── Xây dựng ghi chú ─────────────────────────────────────────────────
        // Nếu có thiết kế 3D → tự động prepend "[BÁNH 3D] Kích thước: ..."
        // để mỗi lần thợ mở đơn sẽ LUÔN thấy kích thước ngay đầu ghi chú.
        String ghiChuCuoiCung;
        if (designMap != null) {
            ghiChuCuoiCung = cakeDesignValidator.buildGhiChuKichThuoc(designMap, request.getGhiChu());
        } else {
            ghiChuCuoiCung = request.getGhiChu();
        }

        // ── Tạo đơn hàng ─────────────────────────────────────────────────────
        DonHang donHang = new DonHang();
        donHang.setKhachHang(khachHang);
        donHang.setDiaChiGiao(request.getDiaChiGiaoHang());
        donHang.setNgayGiaoDuKien(request.getNgayGiaoHang().atTime(12, 0));
        donHang.setTongTien(tongTienThanhToan);
        donHang.setGhiChu(ghiChuCuoiCung);          // ← ghi chú đã ghép kích thước
        donHang.setTrangThai("CHO_XAC_NHAN");
        donHang.setNguonDon("ONLINE");

        // T055 – Lưu JSON thiết kế 3D (nếu có)
        if (designMap != null) {
            try {
                // Lưu lại dạng chuỗi JSON đã được parse-rồi-stringify để đảm bảo sạch
                donHang.setThietKeBanhJson(objectMapper.writeValueAsString(designMap));
            } catch (Exception e) {
                // Không bao giờ xảy ra vì designMap đến từ parse thành công ở trên
                donHang.setThietKeBanhJson(request.getCakeDesignJson());
            }
        }

        DonHang savedDonHang = donHangRepository.save(donHang);

        // ── Lưu chi tiết đơn ─────────────────────────────────────────────────
        List<ChiTietDonHang> chiTietList = request.getItems().stream().map(itemDto -> {
            SanPham sanPham = sanPhamRepository.findById(itemDto.getSanPhamId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));
            ChiTietDonHang chiTiet = new ChiTietDonHang();
            chiTiet.setDonHang(savedDonHang);
            chiTiet.setSanPham(sanPham);
            chiTiet.setSoLuong(itemDto.getSoLuong());
            chiTiet.setDonGiaTaiThoiDiem(BigDecimal.valueOf(itemDto.getDonGia()));
            return chiTiet;
        }).collect(Collectors.toList());

        chiTietDonHangRepository.saveAll(chiTietList);
        savedDonHang.setChiTietDonHangs(chiTietList);

        // ── Thông báo admin ───────────────────────────────────────────────────
        String loiNhanAdmin = designMap != null
                ? "🎂 [BÁNH 3D] Đơn hàng mới HD-" + savedDonHang.getId() + " có thiết kế bánh 3D!"
                : "TING TING! Có đơn hàng mới được đặt: HD-" + savedDonHang.getId();
        notificationService.notifyNewOrderToAdmins(loiNhanAdmin);

        return mapToResponseDto(savedDonHang);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. XEM LỊCH SỬ ĐƠN HÀNG
    // ═══════════════════════════════════════════════════════════════════════════
    public List<OrderDto.Response> getOrdersByUser(String email) {
        NguoiDung khachHang = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại."));
        return donHangRepository.findByKhachHangOrderByNgayTaoDesc(khachHang)
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
    public OrderDto.Response getOrderById(Long id) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng mã số: " + id));
        return mapToResponseDto(donHang);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 5. XỬ LÝ ĐƠN HÀNG (Admin / Nhân viên)
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public OrderDto.Response processOrder(Long id, OrderProcessDto request, String emailNhanVien) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));

        NguoiDung nhanVien = nguoiDungRepository.findByEmail(emailNhanVien)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên xử lý!"));

        donHang.setNhanVien(nhanVien);

        String trangThaiMoi    = request.getTrangThai().toUpperCase();
        String trangThaiHienTai = donHang.getTrangThai();

        if (trangThaiHienTai.equals("HOAN_THANH") || trangThaiHienTai.equals("DA_HUY")) {
            throw new BusinessException("Đơn hàng đã chốt (Giao/Hủy/Hoàn tiền) thì không thể thay đổi trạng thái được nữa!");
        }
        if (trangThaiHienTai.equals("DANG_GIAO") &&
                (trangThaiMoi.equals("CHO_XAC_NHAN") || trangThaiMoi.equals("DANG_CHUAN_BI"))) {
            throw new BusinessException("Đơn hàng đang giao, không thể lùi trạng thái!");
        }

        donHang.setTrangThai(trangThaiMoi);

        if ("DA_HUY".equals(trangThaiMoi)) {
            if (request.getLyDoHuy() == null || request.getLyDoHuy().trim().isEmpty()) {
                throw new RuntimeException("Bắt buộc phải nhập lý do khi hủy đơn hàng!");
            }
            donHang.setLyDoHuy(request.getLyDoHuy());
        }

        DonHang updatedDonHang = donHangRepository.save(donHang);

        if (updatedDonHang.getKhachHang() != null &&
                !"khachvanglai@gmail.com".equals(updatedDonHang.getKhachHang().getEmail())) {
            String loiNhan = "Đơn hàng HD-" + id + " của bạn vừa chuyển sang trạng thái: " + trangThaiMoi;
            if ("DA_HUY".equals(trangThaiMoi)) {
                loiNhan += ". Lý do: " + request.getLyDoHuy();
            }
            notificationService.notifyOrderStatusToUser(updatedDonHang.getKhachHang().getEmail(), loiNhan);
        }

        return mapToResponseDto(updatedDonHang);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 6. HỦY ĐƠN HÀNG (Khách tự hủy)
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public void cancelOrder(Long id, String emailNguoiDung) {
        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailNguoiDung)
                .orElseThrow(() -> new RuntimeException("Tài khoản không hợp lệ."));

        DonHang donHang = donHangRepository.findByIdAndKhachHang(id, khachHang)
                .orElseThrow(() -> new RuntimeException(
                        "Đơn hàng không thuộc quyền sở hữu của bạn hoặc không tồn tại."));

        if (!"CHO_XAC_NHAN".equals(donHang.getTrangThai())) {
            throw new RuntimeException("Đơn hàng đã được xử lý, không thể tự hủy vào lúc này!");
        }

        donHang.setTrangThai("DA_HUY");
        donHang.setLyDoHuy("Khách hàng tự hủy trên web");
        donHangRepository.save(donHang);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 7. T055 – LẤY DỮ LIỆU THIẾT KẾ 3D (đã có từ trước, giữ nguyên)
    // ═══════════════════════════════════════════════════════════════════════════
    public Map<String, Object> get3DCakeDesign(Long orderId) {
        DonHang donHang = donHangRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng có ID: " + orderId));

        String emailUserHienTai = SecurityContextHolder.getContext().getAuthentication().getName();
        NguoiDung userHienTai = nguoiDungRepository.findByEmail(emailUserHienTai)
                .orElseThrow(() -> new RuntimeException("Lỗi xác thực: Không tìm thấy tài khoản!"));

        // Chặn IDOR: khách chỉ xem đơn của mình
        if ("KHACH_HANG".equals(userHienTai.getQuyen())) {
            if (donHang.getKhachHang() == null || !donHang.getKhachHang().getId().equals(userHienTai.getId())) {
                throw new RuntimeException("Bạn không có quyền xem thiết kế của đơn hàng này!");
            }
        }

        String designJson = donHang.getThietKeBanhJson();
        if (designJson == null || designJson.trim().isEmpty()) {
            throw new RuntimeException("Đơn hàng này không có dữ liệu thiết kế 3D");
        }

        try {
            return objectMapper.readValue(designJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi parse dữ liệu 3D Design: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEPAY WEBHOOK: Cập nhật trạng thái sau khi thanh toán
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public void updatePaymentStatus(Long orderId) {
        DonHang donHang = donHangRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        thanhToanRepository.findByDonHang(donHang).ifPresent(tt -> {
            tt.setTrangThai("THANH_CONG");
            tt.setThoiDiemThanhToan(LocalDateTime.now());
            thanhToanRepository.save(tt);
        });

        donHang.setTrangThai("DA_XAC_NHAN");
        donHangRepository.save(donHang);

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

        dto.setTrangThai(donHang.getTrangThai());
        dto.setGhiChu(donHang.getGhiChu());

        if (donHang.getNhanVien() != null) {
            dto.setTenNhanVienPhuTrach(donHang.getNhanVien().getHoTen());
        }
        dto.setLyDoHuy(donHang.getLyDoHuy());

        // T055 – Cho FE biết đơn có thiết kế 3D không (để hiện nút "Xem 3D")
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
                        return itemDto;
                    }).collect(Collectors.toList());
            dto.setItems(itemDtos);
        }

        return dto;
    }
}