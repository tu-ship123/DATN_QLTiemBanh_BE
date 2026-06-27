package com.poly.cake.service;

import com.poly.cake.dto.OrderDto;
import com.poly.cake.entity.*;
import com.poly.cake.repository.*;
import com.poly.cake.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final DonHangRepository donHangRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final SanPhamRepository sanPhamRepository;
    private final NotificationService notificationService;

    // 1. GET FILTER NÂNG CAO
    public List<OrderDto.Response> getFilteredOrders(String trangThai, String nguonDon, LocalDateTime tuNgay, LocalDateTime denNgay) {
        return donHangRepository.filterAdminOrders(trangThai, nguonDon, tuNgay, denNgay)
                .stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    // 2. PUT OVERRIDE STATUS + GHI MINI AUDIT LOG
    @Transactional
    public OrderDto.Response overrideOrderStatus(Long id, TrangThaiDonHang trangThaiMoi, String lyDo, String emailAdmin) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + id));

        NguoiDung admin = nguoiDungRepository.findByEmail(emailAdmin)
                .orElseThrow(() -> new ResourceNotFoundException("Lỗi xác thực Admin"));

        // Lưu lại trạng thái cũ bằng cách gọi .name() để ghi log
        String trangThaiCu = donHang.getTrangThai().name();

        // Gán trực tiếp Enum mới vào, code siêu sạch và an toàn 100%
        donHang.setTrangThai(trangThaiMoi);

        appendMiniAuditLog(donHang, admin.getHoTen(), "Ép đổi trạng thái từ " + trangThaiCu + " sang " + trangThaiMoi.name() + ". Lý do: " + lyDo);

        DonHang updatedDonHang = donHangRepository.save(donHang);
        notifyUser(updatedDonHang, "Đơn hàng HD-" + id + " của bạn đã được Admin xử lý: " + trangThaiMoi.name());

        return mapToResponseDto(updatedDonHang);
    }

    // 3. POST REFUND (HOÀN TIỀN)
    @Transactional
    public OrderDto.Response refundOrder(Long id, String lyDo, String emailAdmin) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + id));

        NguoiDung admin = nguoiDungRepository.findByEmail(emailAdmin)
                .orElseThrow(() -> new ResourceNotFoundException("Lỗi xác thực Admin"));

        donHang.setTrangThai(TrangThaiDonHang.DA_HOAN_TIEN);
        donHang.setLyDoHuy("Hoàn tiền: " + lyDo);

        appendMiniAuditLog(donHang, admin.getHoTen(), "Hoàn tiền cho khách. Lý do: " + lyDo);

        DonHang updatedDonHang = donHangRepository.save(donHang);
        notifyUser(updatedDonHang, "Đơn hàng HD-" + id + " đã được hoàn tiền thành công. Lý do: " + lyDo);

        return mapToResponseDto(updatedDonHang);
    }

    // 4. DELETE CANCEL + ROLLBACK INVENTORY (FIXED RACE CONDITION)
    @Transactional
    public void cancelAndRollbackInventory(Long id, String lyDo, String emailAdmin) {
        DonHang donHang = donHangRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + id));

        NguoiDung admin = nguoiDungRepository.findByEmail(emailAdmin)
                .orElseThrow(() -> new ResourceNotFoundException("Lỗi xác thực Admin"));

        if (donHang.getTrangThai() == TrangThaiDonHang.DA_HUY) {
            throw new BusinessException("Đơn hàng này đã bị hủy từ trước!");
        }

        donHang.setTrangThai(TrangThaiDonHang.DA_HUY);
        donHang.setLyDoHuy("Admin Hủy: " + lyDo);

        appendMiniAuditLog(donHang, admin.getHoTen(), "Hủy đơn & Hoàn số lượng về kho. Lý do: " + lyDo);
        donHangRepository.save(donHang);

        // [FIXED] Rollback tồn kho bằng hàm atomic, an toàn khi nhiều thread chạy song song
        for (ChiTietDonHang ct : donHang.getChiTietDonHangs()) {
            sanPhamRepository.congLaiSoLuongTon(ct.getSanPham().getId(), ct.getSoLuong());
        }

        notifyUser(donHang, "Đơn hàng HD-" + id + " đã bị hủy bởi hệ thống.");
    }

    // --- CÁC HÀM PHỤ TRỢ ---

    private void appendMiniAuditLog(DonHang donHang, String tenAdmin, String action) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String logLine = String.format("\n[AUDIT %s - Admin %s]: %s", time, tenAdmin, action);
        donHang.setGhiChu((donHang.getGhiChu() == null ? "" : donHang.getGhiChu()) + logLine);
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

        if (donHang.getNgayGiaoDuKien() != null) {
            dto.setNgayGiaoHang(donHang.getNgayGiaoDuKien().toLocalDate());
        }

        dto.setNgayTao(donHang.getNgayTao());
        dto.setTongTien(donHang.getTongTien() != null ? donHang.getTongTien().doubleValue() : 0.0);
        dto.setTrangThai(donHang.getTrangThai().toString());
        dto.setGhiChu(donHang.getGhiChu());
        dto.setLyDoHuy(donHang.getLyDoHuy());

        if (donHang.getNhanVien() != null) {
            dto.setTenNhanVienPhuTrach(donHang.getNhanVien().getHoTen());
        }

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