package com.poly.cake.service;

import com.poly.cake.dto.OrderDto;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.ChiTietDonHang;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.SanPham;
import com.poly.cake.repository.DonHangRepository;
import com.poly.cake.repository.ChiTietDonHangRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.SanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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

    // Kéo "loa phát thanh" WebSocket vào đây
    @Autowired
    private NotificationService notificationService;

    // 1. TẠO ĐƠN HÀNG (CHECKOUT)
    @Transactional
    public OrderDto.Response createOrder(OrderDto.Request request, String emailNguoiDung) {
        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailNguoiDung)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản!"));

        // Validate ngày giao hàng
        if (request.getNgayGiaoHang() == null || request.getNgayGiaoHang().isBefore(LocalDate.now())) {
            throw new RuntimeException("Ngày giao hàng không hợp lệ! Phải chọn từ ngày hôm nay trở đi.");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống! Không thể đặt hàng.");
        }

        // Tính toán tiền (Dùng Double tính nháp rồi chuyển sang BigDecimal lưu DB)
        double tongTienHang = request.getItems().stream()
                .mapToDouble(item -> item.getDonGia() * item.getSoLuong())
                .sum();

        double phiShip = (tongTienHang >= 500000) ? 0.0 : 30000.0;
        BigDecimal tongTienThanhToan = BigDecimal.valueOf(tongTienHang + phiShip);

        // Khởi tạo đơn hàng mới
        DonHang donHang = new DonHang();
        donHang.setKhachHang(khachHang);
        donHang.setDiaChiGiao(request.getDiaChiGiaoHang());
        
        // Chuyển LocalDate sang LocalDateTime (Giả sử mặc định giao lúc 12h trưa)
        donHang.setNgayGiaoDuKien(request.getNgayGiaoHang().atTime(12, 0));
        
        donHang.setTongTien(tongTienThanhToan);
        donHang.setGhiChu(request.getGhiChu());
        donHang.setTrangThai("CHO_XAC_NHAN"); 
        donHang.setNguonDon("ONLINE");

        // Lưu đơn hàng (ngayTao sẽ tự động sinh do hàm @PrePersist của bạn)
        DonHang savedDonHang = donHangRepository.save(donHang);

        // Lưu chi tiết đơn hàng
        List<ChiTietDonHang> chiTietList = request.getItems().stream().map(itemDto -> {
            SanPham sanPham = sanPhamRepository.findById(itemDto.getSanPhamId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));

            ChiTietDonHang chiTiet = new ChiTietDonHang();
            chiTiet.setDonHang(savedDonHang);
            chiTiet.setSanPham(sanPham);
            chiTiet.setSoLuong(itemDto.getSoLuong());
            
            // Đã sửa: Dùng setDonGiaTaiThoiDiem và ép kiểu BigDecimal
            chiTiet.setDonGiaTaiThoiDiem(BigDecimal.valueOf(itemDto.getDonGia())); 
            
            return chiTiet;
        }).collect(Collectors.toList());

        chiTietDonHangRepository.saveAll(chiTietList);
        savedDonHang.setChiTietDonHangs(chiTietList);

        // [MỚI THÊM] Bắn thông báo WebSocket cho toàn bộ nhân viên/admin
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

    // 5. CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG 
    @Transactional
    public OrderDto.Response updateStatus(Long id, String trangThaiMoi) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));
        
        donHang.setTrangThai(trangThaiMoi.toUpperCase());
        DonHang updatedDonHang = donHangRepository.save(donHang);

        // [MỚI THÊM] Bắn thông báo trạng thái mới về riêng cho Khách hàng
        if (updatedDonHang.getKhachHang() != null) {
            String loiNhan = "Đơn hàng HD-" + id + " của bạn vừa chuyển sang trạng thái: " + trangThaiMoi;
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
        
        // Bạn có thể thêm notify gửi cho Admin báo khách vừa hủy đơn nếu cần
        // notificationService.notifyNewOrderToAdmins("Khách hàng vừa hủy đơn: HD-" + id);
    }

    // Hàm phụ trợ chuyển đổi Entity thành DTO
    private OrderDto.Response mapToResponseDto(DonHang donHang) {
        OrderDto.Response dto = new OrderDto.Response();
        dto.setId(donHang.getId());
        
        // Entity không lưu mã, mình lấy ID làm mã tạm cho DTO
        dto.setMaDonHang("HD-" + donHang.getId()); 
        
        dto.setDiaChiGiaoHang(donHang.getDiaChiGiao());
        
        // Lấy SĐT từ bảng Khách Hàng (Giả sử Entity NguoiDung có getSoDienThoai)
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

        if (donHang.getChiTietDonHangs() != null) {
            List<OrderDto.OrderItemResponse> itemDtos = donHang.getChiTietDonHangs().stream().map(item -> {
                OrderDto.OrderItemResponse itemDto = new OrderDto.OrderItemResponse();
                itemDto.setSanPhamId(item.getSanPham().getId());
                itemDto.setTenSanPham(item.getSanPham().getTenSanPham());
                itemDto.setSoLuong(item.getSoLuong());
                
                // Đã sửa: Dùng getDonGiaTaiThoiDiem và xử lý null an toàn
                itemDto.setGiaBan(item.getDonGiaTaiThoiDiem() != null ? item.getDonGiaTaiThoiDiem().doubleValue() : 0.0);
                
                return itemDto;
            }).collect(Collectors.toList());
            dto.setItems(itemDtos);
        }
        return dto;
    }
    public Map<String, Object> get3DCakeDesign(Long orderId) {
        // 1. Tìm đơn hàng
        DonHang donHang = donHangRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng có ID: " + orderId));

        // 2. Lấy thông tin người đang gọi API (người đang đăng nhập)
        String emailUserHienTai = SecurityContextHolder.getContext().getAuthentication().getName();
        NguoiDung userHienTai = nguoiDungRepository.findByEmail(emailUserHienTai)
                .orElseThrow(() -> new RuntimeException("Lỗi xác thực: Không tìm thấy tài khoản!"));

        // 3. CHẶN LỖ HỔNG BẢO MẬT (IDOR)
        // Nếu là "Khách hàng", bắt buộc ID của khách hàng trong đơn phải trùng với ID người đang đăng nhập
        if ("KHACH_HANG".equals(userHienTai.getQuyen())) {
            if (donHang.getKhachHang() == null || !donHang.getKhachHang().getId().equals(userHienTai.getId())) {
                throw new RuntimeException("Bạn không có quyền xem thiết kế của đơn hàng này!");
            }
        }
        // (Nếu là Nhân viên hoặc Admin thì nó sẽ bỏ qua đoạn check ở trên và đi tiếp xuống dưới)

        // 4. Lấy chuỗi JSON thiết kế
        String designJson = donHang.getThietKeBanhJson();

        if (designJson == null || designJson.trim().isEmpty()) {
            throw new RuntimeException("Đơn hàng này không có dữ liệu thiết kế 3D");
        }

        // 5. Parse chuỗi String thành Map (Cấu trúc JSON đầy đủ)
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(designJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi parse dữ liệu 3D Design: " + e.getMessage());
        }
    }
}