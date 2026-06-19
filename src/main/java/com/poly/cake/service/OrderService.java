package com.poly.cake.service;

import com.poly.cake.dto.OrderDto;
import com.poly.cake.dto.OrderProcessDto;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.ChiTietDonHang;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.SanPham;
import com.poly.cake.repository.DonHangRepository;
import com.poly.cake.repository.ChiTietDonHangRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.SanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    // 1. TẠO ĐƠN HÀNG (CHECKOUT)
    @Transactional
    public OrderDto.Response createOrder(OrderDto.Request request, String emailNguoiDung) {
        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailNguoiDung)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản!"));

        if (request.getNgayGiaoHang() == null || request.getNgayGiaoHang().isBefore(LocalDate.now())) {
            throw new RuntimeException("Ngày giao hàng không hợp lệ! Phải chọn từ ngày hôm nay trở đi.");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống! Không thể đặt hàng.");
        }

        double tongTienHang = request.getItems().stream()
                .mapToDouble(item -> item.getDonGia() * item.getSoLuong())
                .sum();

        double phiShip = (tongTienHang >= 500000) ? 0.0 : 30000.0;
        BigDecimal tongTienThanhToan = BigDecimal.valueOf(tongTienHang + phiShip);

        DonHang donHang = new DonHang();
        donHang.setKhachHang(khachHang);
        donHang.setDiaChiGiao(request.getDiaChiGiaoHang());
        donHang.setNgayGiaoDuKien(request.getNgayGiaoHang().atTime(12, 0));
        donHang.setTongTien(tongTienThanhToan);
        donHang.setGhiChu(request.getGhiChu());
        donHang.setTrangThai("CHO_XAC_NHAN"); 
        donHang.setNguonDon("ONLINE");

        DonHang savedDonHang = donHangRepository.save(donHang);

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

        notificationService.notifyNewOrderToAdmins("TING TING! Có đơn hàng mới được đặt: HD-" + savedDonHang.getId());

        return mapToResponseDto(savedDonHang);
    }

    // 2. XEM LỊCH SỬ ĐƠN HÀNG 
    public List<OrderDto.Response> getOrdersByUser(String email) {
        NguoiDung khachHang = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại."));
        return donHangRepository.findByKhachHangOrderByNgayTaoDesc(khachHang)
                .stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    // 3. XEM TOÀN BỘ ĐƠN HÀNG 
    public List<OrderDto.Response> getAllOrders() {
        return donHangRepository.findAll().stream()
                .map(this::mapToResponseDto).collect(Collectors.toList());
    }

    // 4. XEM CHI TIẾT ĐƠN HÀNG BY ID
    public OrderDto.Response getOrderById(Long id) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng mã số: " + id));
        return mapToResponseDto(donHang);
    }

    // 5. XỬ LÝ ĐƠN HÀNG (Dành cho Admin/Nhân viên)
    @Transactional
    public OrderDto.Response processOrder(Long id, OrderProcessDto request, String emailNhanVien) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));
        
        NguoiDung nhanVien = nguoiDungRepository.findByEmail(emailNhanVien)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên xử lý!"));

        // Cập nhật người phụ trách
        donHang.setNhanVien(nhanVien);
        
        String trangThaiMoi = request.getTrangThai().toUpperCase();
        donHang.setTrangThai(trangThaiMoi);

        // Bắt lỗi Hủy đơn phải có lý do
        if ("DA_HUY".equals(trangThaiMoi)) {
            if (request.getLyDoHuy() == null || request.getLyDoHuy().trim().isEmpty()) {
                throw new RuntimeException("Bắt buộc phải nhập lý do khi hủy đơn hàng!");
            }
            donHang.setLyDoHuy(request.getLyDoHuy());
        }

        DonHang updatedDonHang = donHangRepository.save(donHang);

        // Bắn thông báo về cho khách (Loại trừ khách vãng lai của POS)
        if (updatedDonHang.getKhachHang() != null && !"khachvanglai@gmail.com".equals(updatedDonHang.getKhachHang().getEmail())) {
            String loiNhan = "Đơn hàng HD-" + id + " của bạn vừa chuyển sang trạng thái: " + trangThaiMoi;
            if ("DA_HUY".equals(trangThaiMoi)) {
                loiNhan += ". Lý do: " + request.getLyDoHuy();
            }
            notificationService.notifyOrderStatusToUser(updatedDonHang.getKhachHang().getEmail(), loiNhan);
        }

        return mapToResponseDto(updatedDonHang);
    }

    // 6. HỦY ĐƠN HÀNG 
    @Transactional
    public void cancelOrder(Long id, String emailNguoiDung) {
        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailNguoiDung)
                .orElseThrow(() -> new RuntimeException("Tài khoản không hợp lệ."));

        DonHang donHang = donHangRepository.findByIdAndKhachHang(id, khachHang)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không thuộc quyền sở hữu của bạn hoặc không tồn tại."));

        if (!"CHO_XAC_NHAN".equals(donHang.getTrangThai())) {
            throw new RuntimeException("Đơn hàng đã được xử lý, không thể tự hủy vào lúc này!");
        }

        donHang.setTrangThai("DA_HUY");
        donHang.setLyDoHuy("Khách hàng tự hủy trên web");
        donHangRepository.save(donHang);
    }

    // Hàm phụ trợ chuyển đổi Entity thành DTO
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
        
        // Set thêm nhân viên phụ trách và lý do hủy vào DTO
        if (donHang.getNhanVien() != null) {
            dto.setTenNhanVienPhuTrach(donHang.getNhanVien().getHoTen());
        }
        dto.setLyDoHuy(donHang.getLyDoHuy());

        if (donHang.getChiTietDonHangs() != null) {
            List<OrderDto.OrderItemResponse> itemDtos = donHang.getChiTietDonHangs().stream().map(item -> {
                OrderDto.OrderItemResponse itemDto = new OrderDto.OrderItemResponse();
                itemDto.setSanPhamId(item.getSanPham().getId());
                itemDto.setTenSanPham(item.getSanPham().getTenSanPham());
                itemDto.setSoLuong(item.getSoLuong());
                itemDto.setGiaBan(item.getDonGiaTaiThoiDiem() != null ? item.getDonGiaTaiThoiDiem().doubleValue() : 0.0);
                return itemDto;
            }).collect(Collectors.toList());
            dto.setItems(itemDtos);
        }
        return dto;
    }
}