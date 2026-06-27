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
            throw new BusinessException("Giỏ hàng trống!");
        }

        // Kiểm tra tồn kho trước khi đặt
        for (OrderDto.OrderItemRequest item : request.getItems()) {
            int updated = sanPhamRepository.truSoLuongTon(item.getSanPhamId(), item.getSoLuong());
            if (updated == 0) {
                throw new BusinessException("Sản phẩm ID " + item.getSanPhamId() + " không đủ số lượng!");
            }
        }

        double tongTienHang = request.getItems().stream()
                .mapToDouble(item -> item.getDonGia() * item.getSoLuong()).sum();
        BigDecimal tongTien = BigDecimal.valueOf(tongTienHang + (tongTienHang >= 500000 ? 0.0 : 30000.0));

        DonHang donHang = new DonHang();
        donHang.setKhachHang(khachHang);
        donHang.setDiaChiGiao(request.getDiaChiGiaoHang());
        donHang.setNgayGiaoDuKien(request.getNgayGiaoHang().atTime(12, 0));
        donHang.setTongTien(tongTien);
        donHang.setTrangThai(TrangThaiDonHang.CHO_XAC_NHAN);
        donHang.setNguonDon("ONLINE");

        DonHang savedDonHang = donHangRepository.save(donHang);

        // ... (Mapping chi tiết đơn hàng giữ nguyên như cũ của em)
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

    // MAPPING DTO
    private OrderDto.Response mapToResponseDto(DonHang donHang) {
        OrderDto.Response dto = new OrderDto.Response();
        // ... giữ nguyên logic mapping của em ở đây
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