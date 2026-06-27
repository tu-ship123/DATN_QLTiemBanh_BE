package com.poly.cake.service;

import com.poly.cake.dto.*;
import com.poly.cake.entity.*;
import com.poly.cake.exception.*;
import com.poly.cake.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final ObjectMapper objectMapper;

    // 1. TẠO ĐƠN HÀNG (CHECKOUT)
    @Transactional
    public OrderDto.Response createOrder(OrderDto.Request request, String emailNguoiDung) {
        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailNguoiDung)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản!"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Giỏ hàng trống! Không thể đặt hàng.");
        }

        // =========================================================
        // BƯỚC 1: KIỂM TRA TỒN KHO TRƯỚC (Chỉ đọc dữ liệu, tuyệt đối không trừ)
        // =========================================================
        for (OrderDto.OrderItemRequest item : request.getItems()) {
            SanPham sp = sanPhamRepository.findById(item.getSanPhamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm ID " + item.getSanPhamId() + " không tồn tại!"));

            if (sp.getSoLuongTon() < item.getSoLuong()) {
                throw new BusinessException("Sản phẩm [" + sp.getTenSanPham() + "] không đủ số lượng trong kho!");
            }
        }

        // Tính toán tổng tiền
        double tongTienHang = request.getItems().stream()
                .mapToDouble(item -> item.getDonGia() * item.getSoLuong())
                .sum();
        BigDecimal tongTienThanhToan = BigDecimal.valueOf(tongTienHang + (tongTienHang >= 500000 ? 0.0 : 30000.0));

        // =========================================================
        // BƯỚC 2: LƯU ĐƠN HÀNG VÀ CHI TIẾT VÀO DATABASE
        // =========================================================
        DonHang donHang = new DonHang();
        donHang.setKhachHang(khachHang);
        donHang.setDiaChiGiao(request.getDiaChiGiaoHang());
        if (request.getNgayGiaoHang() != null) {
            donHang.setNgayGiaoDuKien(request.getNgayGiaoHang().atTime(12, 0));
        }
        donHang.setTongTien(tongTienThanhToan);
        donHang.setTrangThai(TrangThaiDonHang.CHO_XAC_NHAN);
        donHang.setNguonDon("ONLINE");

        DonHang savedDonHang = donHangRepository.save(donHang);

        List<ChiTietDonHang> chiTietList = request.getItems().stream().map(itemDto -> {
            // Bước 1 đã check tồn tại rồi nên bước này get() luôn không sợ lỗi
            SanPham sanPham = sanPhamRepository.findById(itemDto.getSanPhamId()).get();

            ChiTietDonHang chiTiet = new ChiTietDonHang();
            chiTiet.setDonHang(savedDonHang);
            chiTiet.setSanPham(sanPham);
            chiTiet.setSoLuong(itemDto.getSoLuong());
            chiTiet.setDonGiaTaiThoiDiem(BigDecimal.valueOf(itemDto.getDonGia()));
            return chiTiet;
        }).collect(Collectors.toList());

        chiTietDonHangRepository.saveAll(chiTietList);
        savedDonHang.setChiTietDonHangs(chiTietList);

        // =========================================================
        // BƯỚC 3: TRỪ TỒN KHO THỰC TẾ (Dùng lệnh Update Atomic)
        // =========================================================
        for (OrderDto.OrderItemRequest item : request.getItems()) {
            int updated = sanPhamRepository.truSoLuongTon(item.getSanPhamId(), item.getSoLuong());
            // Đề phòng đúng tích tắc đó có người khác mua mất hàng (Race Condition)
            if (updated == 0) {
                // Ném exception ra đây thì @Transactional sẽ lập tức ROLLBACK (Xóa luôn đơn hàng vừa tạo ở Bước 2)
                throw new BusinessException("Rất tiếc, sản phẩm ID " + item.getSanPhamId() + " vừa hết hàng trong tích tắc! Vui lòng thử lại.");
            }
        }

        // Bắn thông báo về cho Admin
        notificationService.notifyNewOrderToAdmins("TING TING! Có đơn hàng mới được đặt: HD-" + savedDonHang.getId());

        return mapToResponseDto(savedDonHang);
    }
    // 2 & 3. LẤY DANH SÁCH ĐƠN HÀNG
    public List<OrderDto.Response> getOrdersByUser(String email) {
        NguoiDung khachHang = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại."));
        return donHangRepository.findByKhachHangOrderByNgayTaoDesc(khachHang)
                .stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    public List<OrderDto.Response> getAllOrders() {
        return donHangRepository.findAll().stream()
                .map(this::mapToResponseDto).collect(Collectors.toList());
    }

    // 4. XEM CHI TIẾT (IDOR CHECK)
    public OrderDto.Response getOrderById(Long id, String email, String role) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + id));

        if ("ROLE_KHACH_HANG".equals(role)) {
            if (donHang.getKhachHang() == null || !donHang.getKhachHang().getEmail().equals(email)) {
                throw new ForbiddenException("Bạn không có quyền xem đơn hàng này!");
            }
        }
        return mapToResponseDto(donHang);
    }

    // 5. XỬ LÝ ĐƠN HÀNG (TRẠNG THÁI)
    @Transactional
    public OrderDto.Response processOrder(Long id, OrderProcessDto request, String emailNhanVien) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại."));

        // Kiểm tra trạng thái bằng Enum
        if (donHang.getTrangThai() == TrangThaiDonHang.DA_GIAO || donHang.getTrangThai() == TrangThaiDonHang.DA_HUY) {
            throw new BusinessException("Đơn hàng đã kết thúc, không thể thay đổi!");
        }

        donHang.setTrangThai(request.getTrangThai());
        donHangRepository.save(donHang);

        return mapToResponseDto(donHang);
    }

    // 6. HỦY ĐƠN
    @Transactional
    public void cancelOrder(Long id, String emailNguoiDung) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại."));

        if (donHang.getTrangThai() != TrangThaiDonHang.CHO_XAC_NHAN) {
            throw new BusinessException("Đơn hàng không thể hủy lúc này!");
        }

        donHang.setTrangThai(TrangThaiDonHang.DA_HUY);
        donHangRepository.save(donHang);

        // Rollback kho
        for (ChiTietDonHang ct : donHang.getChiTietDonHangs()) {
            sanPhamRepository.congLaiSoLuongTon(ct.getSanPham().getId(), ct.getSoLuong());
        }
    }
    // MAPPING DTO HOÀN CHỈNH - KHÔNG CÒN PLACEHOLDER
    private OrderDto.Response mapToResponseDto(DonHang donHang) {
        OrderDto.Response dto = new OrderDto.Response();

        // Mapping thông tin đơn hàng
        dto.setId(donHang.getId());
        dto.setMaDonHang("HD-" + donHang.getId());
        dto.setDiaChiGiaoHang(donHang.getDiaChiGiao());
        dto.setGhiChu(donHang.getGhiChu());
        dto.setLyDoHuy(donHang.getLyDoHuy());
        dto.setNgayTao(donHang.getNgayTao());

        // Mapping thông tin khách hàng an toàn
        if (donHang.getKhachHang() != null) {
            dto.setSoDienThoai(donHang.getKhachHang().getSoDienThoai());
            dto.setEmailNguoiDung(donHang.getKhachHang().getEmail());
        }

        // Mapping ngày giao và tổng tiền
        if (donHang.getNgayGiaoDuKien() != null) {
            dto.setNgayGiaoHang(donHang.getNgayGiaoDuKien().toLocalDate());
        }
        if (donHang.getTongTien() != null) {
            dto.setTongTien(donHang.getTongTien().doubleValue());
        }

        // Mapping trạng thái (Enum -> String)
        dto.setTrangThai(donHang.getTrangThai() != null ? donHang.getTrangThai().name() : "CHO_XAC_NHAN");

        // Mapping tên nhân viên phụ trách
        if (donHang.getNhanVien() != null) {
            dto.setTenNhanVienPhuTrach(donHang.getNhanVien().getHoTen());
        }

        // Mapping danh sách sản phẩm (Items)
        if (donHang.getChiTietDonHangs() != null) {
            List<OrderDto.OrderItemResponse> itemDtos = donHang.getChiTietDonHangs().stream()
                    .map(ct -> {
                        OrderDto.OrderItemResponse itemDto = new OrderDto.OrderItemResponse();
                        itemDto.setSanPhamId(ct.getSanPham().getId());
                        itemDto.setTenSanPham(ct.getSanPham().getTenSanPham());
                        itemDto.setSoLuong(ct.getSoLuong());
                        // Đảm bảo không bị null pointer ở giá
                        itemDto.setGiaBan(ct.getDonGiaTaiThoiDiem() != null ? ct.getDonGiaTaiThoiDiem().doubleValue() : 0.0);
                        return itemDto;
                    })
                    .collect(Collectors.toList());
            dto.setItems(itemDtos);
        }

        return dto;
    }

    public Map<String, Object> get3DCakeDesign(Long orderId) {
        DonHang donHang = donHangRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        // IDOR Check
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        NguoiDung user = nguoiDungRepository.findByEmail(email).orElseThrow();
        if ("KHACH_HANG".equals(user.getQuyen()) && (donHang.getKhachHang() == null || !donHang.getKhachHang().getId().equals(user.getId()))) {
            throw new ForbiddenException("Bạn không có quyền xem!");
        }

        try {
            return objectMapper.readValue(donHang.getThietKeBanhJson(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new BusinessException("Lỗi dữ liệu thiết kế 3D");
        }
    }
}