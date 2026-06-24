package com.poly.cake.service;

import com.poly.cake.dto.OrderDto;
import com.poly.cake.dto.OrderProcessDto;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.ChiTietDonHang;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.SanPham;
import com.poly.cake.entity.ThanhToan;
import com.poly.cake.repository.DonHangRepository;
import com.poly.cake.repository.ChiTietDonHangRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.SanPhamRepository;
import com.poly.cake.repository.ThanhToanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.poly.cake.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ThanhToanRepository thanhToanRepository;

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
        String trangThaiHienTai = donHang.getTrangThai();


        // [SỬA LỖI 14]: Chặn lùi trạng thái đơn hàng
        if (trangThaiHienTai.equals("HOAN_THANH") || trangThaiHienTai.equals("DA_HUY")) {
            throw new BusinessException("Đơn hàng đã chốt (Giao/Hủy/Hoàn tiền) thì không thể thay đổi trạng thái được nữa!");
        }

        // Chặn chuyển trạng thái ngược luồng (Ví dụ: Đang giao -> Chờ xác nhận)
        if (trangThaiHienTai.equals("DANG_GIAO") && (trangThaiMoi.equals("CHO_XAC_NHAN") || trangThaiMoi.equals("DANG_CHUAN_BI"))) {
            throw new BusinessException("Đơn hàng đang giao, không thể lùi trạng thái!");
        }
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

    // ===== SEPAY WEBHOOK: CẬP NHẬT TRẠNG THÁI SAU KHI THANH TOÁN =====
    @Transactional
    public void updatePaymentStatus(Long orderId) {
        // 1. Tìm đơn hàng
        DonHang donHang = donHangRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        // 2. Cập nhật bảng thanh_toan -> THANH_CONG (nếu tồn tại)
        thanhToanRepository.findByDonHang(donHang).ifPresent(tt -> {
            tt.setTrangThai("THANH_CONG");
            tt.setThoiDiemThanhToan(LocalDateTime.now());
            thanhToanRepository.save(tt);
        });

        // 3. Cập nhật trạng thái đơn hàng -> DA_XAC_NHAN
        donHang.setTrangThai("DA_XAC_NHAN");
        donHangRepository.save(donHang);

        // 4. Thông báo cho admin
        notificationService.notifyNewOrderToAdmins(
                "✅ Đơn hàng DH" + orderId + " đã thanh toán qua SePay, chuyển sang DA_XAC_NHAN!"
        );
    }
}