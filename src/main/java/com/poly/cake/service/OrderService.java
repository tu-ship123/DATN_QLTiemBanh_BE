package com.poly.cake.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.cake.dto.OrderDto;
import com.poly.cake.dto.OrderProcessDto;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.ChiTietDonHang;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.SanPham;
import com.poly.cake.entity.TrangThaiDonHang; // Đã thêm import Enum
import com.poly.cake.exception.BusinessException;
import com.poly.cake.exception.ForbiddenException;
import com.poly.cake.exception.ResourceNotFoundException;
import com.poly.cake.repository.DonHangRepository;
import com.poly.cake.repository.ChiTietDonHangRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.SanPhamRepository;
import com.poly.cake.repository.ThanhToanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // T055 – Validator thiết kế bánh 3D
    private final CakeDesignValidator cakeDesignValidator;

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
        double phiShip = (tongTienHang >= 500_000) ? 0.0 : 30_000.0;
        BigDecimal tongTienThanhToan = BigDecimal.valueOf(tongTienHang + phiShip);

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

        if (designMap != null) {
            try {
                donHang.setThietKeBanhJson(objectMapper.writeValueAsString(designMap));
            } catch (Exception e) {
                donHang.setThietKeBanhJson(request.getCakeDesignJson());
            }
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
            return chiTiet;
        }).collect(Collectors.toList());

        chiTietDonHangRepository.saveAll(chiTietList);
        savedDonHang.setChiTietDonHangs(chiTietList);

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
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại."));
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
    public OrderDto.Response getOrderById(Long id, String email, String role) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + id));

        if ("ROLE_KHACH_HANG".equals(role) || "KHACH_HANG".equals(role)) {
            if (donHang.getKhachHang() == null || !donHang.getKhachHang().getEmail().equals(email)) {
                throw new ForbiddenException("Bạn không có quyền xem đơn hàng này!");
            }
        }

        return mapToResponseDto(donHang);
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
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public void cancelOrder(Long id, String emailNguoiDung) {
        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailNguoiDung)
                .orElseThrow(() -> new BusinessException("Tài khoản không hợp lệ."));

        DonHang donHang = donHangRepository.findByIdAndKhachHang(id, khachHang)
                .orElseThrow(() -> new ForbiddenException(
                        "Đơn hàng không thuộc quyền sở hữu của bạn hoặc không tồn tại."));

        // Dùng toán tử == để so sánh Enum
        if (donHang.getTrangThai() != TrangThaiDonHang.CHO_XAC_NHAN) {
            throw new BusinessException("Đơn hàng đã được xử lý, không thể tự hủy vào lúc này!");
        }

        donHang.setTrangThai(TrangThaiDonHang.DA_HUY);
        donHang.setLyDoHuy("Khách hàng tự hủy trên web");
        donHangRepository.save(donHang);
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
                        return itemDto;
                    }).collect(Collectors.toList());
            dto.setItems(itemDtos);
        }

        return dto;
    }
}