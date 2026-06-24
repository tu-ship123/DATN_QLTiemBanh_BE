package com.poly.cake.service;

import com.poly.cake.dto.OrderDto;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.ChiTietDonHang;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.SanPham;
import com.poly.cake.repository.DonHangRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.SanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminOrderService {

    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private NotificationService notificationService;

    // 1. GET FILTER NÂNG CAO
    public List<OrderDto.Response> getFilteredOrders(String trangThai, String nguonDon, LocalDateTime tuNgay, LocalDateTime denNgay) {
        return donHangRepository.filterAdminOrders(trangThai, nguonDon, tuNgay, denNgay)
                .stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    // 2. PUT OVERRIDE STATUS + GHI MINI AUDIT LOG
    @Transactional
    public OrderDto.Response overrideOrderStatus(Long id, String trangThaiMoi, String lyDo, String emailAdmin) {
        DonHang donHang = donHangRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        NguoiDung admin = nguoiDungRepository.findByEmail(emailAdmin).orElseThrow(() -> new RuntimeException("Lỗi xác thực Admin"));

        String trangThaiCu = donHang.getTrangThai();
        donHang.setTrangThai(trangThaiMoi.toUpperCase());
        
        appendMiniAuditLog(donHang, admin.getHoTen(), "Ép đổi trạng thái từ " + trangThaiCu + " sang " + trangThaiMoi + ". Lý do: " + lyDo);
        
        DonHang updatedDonHang = donHangRepository.save(donHang);
        notifyUser(updatedDonHang, "Đơn hàng HD-" + id + " của bạn đã được Admin xử lý: " + trangThaiMoi);
        
        return mapToResponseDto(updatedDonHang);
    }

    // 3. POST REFUND (HOÀN TIỀN)
    @Transactional
    public OrderDto.Response refundOrder(Long id, String lyDo, String emailAdmin) {
        DonHang donHang = donHangRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        NguoiDung admin = nguoiDungRepository.findByEmail(emailAdmin).orElseThrow(() -> new RuntimeException("Lỗi xác thực Admin"));

        donHang.setTrangThai("DA_HOAN_TIEN");
        donHang.setLyDoHuy("Hoàn tiền: " + lyDo);
        
        appendMiniAuditLog(donHang, admin.getHoTen(), "Hoàn tiền cho khách. Lý do: " + lyDo);
        
        DonHang updatedDonHang = donHangRepository.save(donHang);
        notifyUser(updatedDonHang, "Đơn hàng HD-" + id + " đã được hoàn tiền thành công. Lý do: " + lyDo);
        
        return mapToResponseDto(updatedDonHang);
    }

    // 4. DELETE CANCEL + ROLLBACK INVENTORY (Hoàn kho)
    @Transactional
    public void cancelAndRollbackInventory(Long id, String lyDo, String emailAdmin) {
        DonHang donHang = donHangRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        NguoiDung admin = nguoiDungRepository.findByEmail(emailAdmin).orElseThrow(() -> new RuntimeException("Lỗi xác thực Admin"));

        if ("DA_HUY".equals(donHang.getTrangThai())) {
            throw new RuntimeException("Đơn hàng này đã bị hủy từ trước!");
        }

        donHang.setTrangThai("DA_HUY");
        donHang.setLyDoHuy("Admin Hủy & Rollback kho: " + lyDo);
        
        appendMiniAuditLog(donHang, admin.getHoTen(), "Hủy đơn ép buộc & Hoàn số lượng về kho. Lý do: " + lyDo);
        
        DonHang updatedDonHang = donHangRepository.save(donHang);

        for (ChiTietDonHang ct : updatedDonHang.getChiTietDonHangs()) {
            SanPham sp = ct.getSanPham();
            sp.setSoLuongTon(sp.getSoLuongTon() + ct.getSoLuong());
            sanPhamRepository.save(sp);
        }

        notifyUser(updatedDonHang, "Đơn hàng HD-" + id + " đã bị hủy bởi hệ thống. Lý do: " + lyDo);
    }

    // 5. POST TẠO ĐƠN HÀNG (DÀNH CHO ADMIN/STAFF)
    @Transactional
    public OrderDto.Response createOrder(OrderDto.Request request, String emailNhanVien) {
        NguoiDung nhanVien = nguoiDungRepository.findByEmail(emailNhanVien)
                .orElseThrow(() -> new RuntimeException("Lỗi xác thực Nhân viên/Admin"));

        DonHang donHang = new DonHang();
        donHang.setDiaChiGiao(request.getDiaChiGiaoHang());
        
        if (request.getNgayGiaoHang() != null) {
            donHang.setNgayGiaoDuKien(request.getNgayGiaoHang().atStartOfDay());
        }
        
        donHang.setGhiChu(request.getGhiChu() != null ? request.getGhiChu() : "");
        donHang.setNgayTao(LocalDateTime.now());
        donHang.setTrangThai("DA_XAC_NHAN");
        donHang.setNhanVien(nhanVien);

        // Xử lý lỗi nullable = false cho cột khach_hang_id
        NguoiDung khachVangLai = nguoiDungRepository.findByEmail("khachvanglai@gmail.com")
                .orElseThrow(() -> new RuntimeException("Lỗi: Trong Database chưa có user khachvanglai@gmail.com. Vui lòng tạo tài khoản này trước!"));
        donHang.setKhachHang(khachVangLai);

        if (request.getSoDienThoai() != null && !request.getSoDienThoai().isEmpty()) {
            donHang.setGhiChu(donHang.getGhiChu() + " | SĐT Khách: " + request.getSoDienThoai());
        }

        BigDecimal tongTien = BigDecimal.ZERO;
        List<ChiTietDonHang> chiTietList = new ArrayList<>();

        if (request.getItems() != null) {
            for (OrderDto.OrderItemRequest itemReq : request.getItems()) {
                SanPham sanPham = sanPhamRepository.findById(itemReq.getSanPhamId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + itemReq.getSanPhamId()));

                if (sanPham.getSoLuongTon() < itemReq.getSoLuong()) {
                    throw new RuntimeException("Sản phẩm '" + sanPham.getTenSanPham() + "' không đủ số lượng tồn kho!");
                }

                sanPham.setSoLuongTon(sanPham.getSoLuongTon() - itemReq.getSoLuong());
                sanPhamRepository.save(sanPham);

                ChiTietDonHang chiTiet = new ChiTietDonHang();
                chiTiet.setDonHang(donHang);
                chiTiet.setSanPham(sanPham);
                chiTiet.setSoLuong(itemReq.getSoLuong());
                
                BigDecimal donGia = BigDecimal.valueOf(itemReq.getDonGia());
                chiTiet.setDonGiaTaiThoiDiem(donGia);

                chiTietList.add(chiTiet);
                
                BigDecimal thanhTien = donGia.multiply(BigDecimal.valueOf(itemReq.getSoLuong()));
                tongTien = tongTien.add(thanhTien);
            }
        }

        donHang.setTongTien(tongTien);
        donHang.setChiTietDonHangs(chiTietList);

        appendMiniAuditLog(donHang, nhanVien.getHoTen(), "Khởi tạo đơn hàng mới từ hệ thống Admin.");

        DonHang savedDonHang = donHangRepository.save(donHang);

        return mapToResponseDto(savedDonHang);
    }

    // --- CÁC HÀM PHỤ TRỢ ---

    private void appendMiniAuditLog(DonHang donHang, String tenAdmin, String action) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String logLine = String.format("\n[AUDIT %s - Admin %s]: %s", time, tenAdmin, action);
        String currentNote = (donHang.getGhiChu() == null) ? "" : donHang.getGhiChu();
        donHang.setGhiChu(currentNote + logLine);
    }

    private void notifyUser(DonHang donHang, String message) {
        if (donHang.getKhachHang() != null && !"khachvanglai@gmail.com".equals(donHang.getKhachHang().getEmail())) {
            notificationService.notifyOrderStatusToUser(donHang.getKhachHang().getEmail(), message);
        }
    }

    private OrderDto.Response mapToResponseDto(DonHang donHang) {
        OrderDto.Response dto = new OrderDto.Response();
        dto.setId(donHang.getId());
        dto.setMaDonHang("HD-" + donHang.getId());
        dto.setDiaChiGiaoHang(donHang.getDiaChiGiao());
        if (donHang.getKhachHang() != null) {
            dto.setSoDienThoai(donHang.getKhachHang().getSoDienThoai());
            dto.setEmailNguoiDung(donHang.getKhachHang().getEmail());
        }
        if (donHang.getNgayGiaoDuKien() != null) dto.setNgayGiaoHang(donHang.getNgayGiaoDuKien().toLocalDate());
        dto.setNgayTao(donHang.getNgayTao());
        if (donHang.getTongTien() != null) dto.setTongTien(donHang.getTongTien().doubleValue());
        dto.setTrangThai(donHang.getTrangThai());
        dto.setGhiChu(donHang.getGhiChu());
        dto.setLyDoHuy(donHang.getLyDoHuy());
        if (donHang.getNhanVien() != null) dto.setTenNhanVienPhuTrach(donHang.getNhanVien().getHoTen());
        
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