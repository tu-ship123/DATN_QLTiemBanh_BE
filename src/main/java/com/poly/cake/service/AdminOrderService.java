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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminOrderService {

    @Autowired private DonHangRepository donHangRepository;
    @Autowired private NguoiDungRepository nguoiDungRepository;
    @Autowired private SanPhamRepository sanPhamRepository;
    @Autowired private NotificationService notificationService;

    // Thứ tự hợp lệ của các trạng thái (flow chuẩn)
    private static final List<String> STATUS_FLOW = List.of(
            "CHO_XAC_NHAN", "DA_XAC_NHAN", "DANG_LAM", "SAN_SANG", "DANG_GIAO", "HOAN_THANH"
    );
    // Trạng thái cuối, không được chuyển tiếp
    private static final Set<String> TERMINAL_STATUSES = Set.of("HOAN_THANH", "DA_HUY", "DA_HOAN_TIEN");

    // ── CŨ ──────────────────────────────────────────────────────────────────────

    public List<OrderDto.Response> getFilteredOrders(String trangThai, String nguonDon,
                                                      LocalDateTime tuNgay, LocalDateTime denNgay) {
        return donHangRepository.filterAdminOrders(trangThai, nguonDon, tuNgay, denNgay)
                .stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    @Transactional
    public OrderDto.Response overrideOrderStatus(Long id, String trangThaiMoi, String lyDo, String emailAdmin) {
        DonHang donHang = findOrder(id);
        NguoiDung admin = findAdmin(emailAdmin);

        String trangThaiCu = donHang.getTrangThai();
        donHang.setTrangThai(trangThaiMoi.toUpperCase());
        appendMiniAuditLog(donHang, admin.getHoTen(),
                "Ép đổi trạng thái từ " + trangThaiCu + " sang " + trangThaiMoi + ". Lý do: " + lyDo);

        DonHang saved = donHangRepository.save(donHang);
        notifyUser(saved, "Đơn hàng HD-" + id + " đã được Admin xử lý: " + trangThaiMoi);
        return mapToResponseDto(saved);
    }

    @Transactional
    public OrderDto.Response refundOrder(Long id, String lyDo, String emailAdmin) {
        DonHang donHang = findOrder(id);
        NguoiDung admin = findAdmin(emailAdmin);

        donHang.setTrangThai("DA_HOAN_TIEN");
        donHang.setLyDoHuy("Hoàn tiền: " + lyDo);
        appendMiniAuditLog(donHang, admin.getHoTen(), "Hoàn tiền cho khách. Lý do: " + lyDo);

        DonHang saved = donHangRepository.save(donHang);
        notifyUser(saved, "Đơn hàng HD-" + id + " đã được hoàn tiền thành công. Lý do: " + lyDo);
        return mapToResponseDto(saved);
    }

    @Transactional
    public void cancelAndRollbackInventory(Long id, String lyDo, String emailAdmin) {
        DonHang donHang = findOrder(id);
        NguoiDung admin = findAdmin(emailAdmin);

        if ("DA_HUY".equals(donHang.getTrangThai())) {
            throw new RuntimeException("Đơn hàng này đã bị hủy từ trước!");
        }

        donHang.setTrangThai("DA_HUY");
        donHang.setLyDoHuy("Admin Hủy & Rollback kho: " + lyDo);
        appendMiniAuditLog(donHang, admin.getHoTen(), "Hủy đơn ép buộc & Hoàn số lượng về kho. Lý do: " + lyDo);

        DonHang saved = donHangRepository.save(donHang);

        for (ChiTietDonHang ct : saved.getChiTietDonHangs()) {
            SanPham sp = ct.getSanPham();
            sp.setSoLuongTon(sp.getSoLuongTon() + ct.getSoLuong());
            sanPhamRepository.save(sp);
        }

        notifyUser(saved, "Đơn hàng HD-" + id + " đã bị hủy bởi hệ thống. Lý do: " + lyDo);
    }

    // ── MỚI 5: Chỉnh sửa thông tin đơn ─────────────────────────────────────────
    /**
     * Chỉnh sửa các trường thông tin của đơn hàng (không đổi trạng thái, không đổi sản phẩm).
     * Chỉ các trường được gửi lên (không null) mới được cập nhật.
     */
    @Transactional
    public OrderDto.Response updateOrderInfo(Long id, OrderDto.UpdateRequest request, String emailAdmin) {
        DonHang donHang = findOrder(id);
        NguoiDung admin = findAdmin(emailAdmin);

        // Chặn chỉnh sửa đơn đã kết thúc
        if (TERMINAL_STATUSES.contains(donHang.getTrangThai())) {
            throw new RuntimeException("Không thể chỉnh sửa đơn hàng ở trạng thái: " + donHang.getTrangThai());
        }

        StringBuilder changes = new StringBuilder("Chỉnh sửa thông tin đơn:");
        boolean hasChange = false;

        if (request.getDiaChiGiaoHang() != null) {
            changes.append(" Địa chỉ=[").append(request.getDiaChiGiaoHang()).append("]");
            donHang.setDiaChiGiao(request.getDiaChiGiaoHang());
            hasChange = true;
        }
        if (request.getSoDienThoai() != null && donHang.getKhachHang() != null) {
            changes.append(" SĐT=[").append(request.getSoDienThoai()).append("]");
            donHang.getKhachHang().setSoDienThoai(request.getSoDienThoai());
            nguoiDungRepository.save(donHang.getKhachHang());
            hasChange = true;
        }
        if (request.getNgayGiaoHang() != null) {
            changes.append(" NgàyGiao=[").append(request.getNgayGiaoHang()).append("]");
            donHang.setNgayGiaoDuKien(request.getNgayGiaoHang().atStartOfDay());
            hasChange = true;
        }
        if (request.getGhiChu() != null) {
            // Ghi chú mới thay thế ghi chú cũ (audit log vẫn được giữ ở cuối)
            String auditPart = extractAuditLog(donHang.getGhiChu());
            donHang.setGhiChu(request.getGhiChu() + (auditPart.isEmpty() ? "" : "\n" + auditPart));
            changes.append(" GhiChú=[đã cập nhật]");
            hasChange = true;
        }

        if (!hasChange) {
            throw new RuntimeException("Không có thông tin nào được thay đổi!");
        }

        appendMiniAuditLog(donHang, admin.getHoTen(), changes.toString());
        return mapToResponseDto(donHangRepository.save(donHang));
    }

    // ── MỚI 6: Đổi trạng thái theo flow chuẩn ──────────────────────────────────
    /**
     * Chuyển trạng thái theo đúng thứ tự flow: CHO_XAC_NHAN → DA_XAC_NHAN → DANG_LAM
     * → SAN_SANG → DANG_GIAO → HOAN_THANH. Hoặc → DA_HUY ở bất kỳ bước nào.
     * Khác với override: endpoint này validate thứ tự hợp lệ.
     */
    @Transactional
    public OrderDto.Response changeOrderStatus(Long id, String trangThaiMoi,
                                               String lyDoHuy, String emailAdmin) {
        DonHang donHang = findOrder(id);
        NguoiDung admin = findAdmin(emailAdmin);
        String trangThaiHienTai = donHang.getTrangThai();
        String trangThaiMoiUpper = trangThaiMoi.toUpperCase();

        // Không được đổi nếu đã ở trạng thái cuối
        if (TERMINAL_STATUSES.contains(trangThaiHienTai)) {
            throw new RuntimeException("Đơn hàng đã ở trạng thái kết thúc: " + trangThaiHienTai
                    + ". Dùng Override nếu thực sự cần.");
        }

        // Hủy được phép ở bất kỳ bước nào
        if ("DA_HUY".equals(trangThaiMoiUpper)) {
            donHang.setTrangThai("DA_HUY");
            donHang.setLyDoHuy(lyDoHuy != null ? lyDoHuy : "Admin hủy");
            appendMiniAuditLog(donHang, admin.getHoTen(),
                    "Hủy đơn theo flow. Lý do: " + donHang.getLyDoHuy());
            DonHang saved = donHangRepository.save(donHang);
            notifyUser(saved, "Đơn hàng HD-" + id + " đã bị hủy. Lý do: " + donHang.getLyDoHuy());
            return mapToResponseDto(saved);
        }

        // Validate: trangThaiMoi phải đứng ngay sau trangThaiHienTai trong flow
        int currentIdx = STATUS_FLOW.indexOf(trangThaiHienTai);
        int nextIdx    = STATUS_FLOW.indexOf(trangThaiMoiUpper);

        if (currentIdx == -1) {
            throw new RuntimeException("Trạng thái hiện tại không hợp lệ trong flow: " + trangThaiHienTai);
        }
        if (nextIdx == -1) {
            throw new RuntimeException("Trạng thái mới không tồn tại trong flow: " + trangThaiMoiUpper);
        }
        if (nextIdx != currentIdx + 1) {
            throw new RuntimeException("Không thể chuyển từ " + trangThaiHienTai + " sang " + trangThaiMoiUpper
                    + ". Bước tiếp theo hợp lệ là: " + STATUS_FLOW.get(currentIdx + 1)
                    + ". Dùng Override nếu cần bỏ qua bước.");
        }

        donHang.setTrangThai(trangThaiMoiUpper);
        appendMiniAuditLog(donHang, admin.getHoTen(),
                "Chuyển trạng thái " + trangThaiHienTai + " → " + trangThaiMoiUpper);

        DonHang saved = donHangRepository.save(donHang);
        notifyUser(saved, "Đơn hàng HD-" + id + " đã chuyển sang: " + trangThaiMoiUpper);
        return mapToResponseDto(saved);
    }

    // ── MỚI 7: Dữ liệu in đơn ──────────────────────────────────────────────────
    /**
     * Trả về toàn bộ thông tin cần thiết để render phiếu in đơn hàng.
     */
    public OrderDto.PrintResponse getPrintData(Long id) {
        DonHang donHang = findOrder(id);

        OrderDto.PrintResponse print = new OrderDto.PrintResponse();
        print.setId(donHang.getId());
        print.setMaDonHang("HD-" + donHang.getId());
        print.setTrangThai(donHang.getTrangThai());
        print.setNgayTao(donHang.getNgayTao());
        print.setNguonDon(donHang.getNguonDon());

        if (donHang.getNgayGiaoDuKien() != null) {
            print.setNgayGiaoHang(donHang.getNgayGiaoDuKien().toLocalDate());
        }

        double tongTien   = donHang.getTongTien()   != null ? donHang.getTongTien().doubleValue()   : 0.0;
        double soTienCoc  = donHang.getSoTienCoc()  != null ? donHang.getSoTienCoc().doubleValue()  : 0.0;
        print.setTongTien(tongTien);
        print.setSoTienCoc(soTienCoc);
        print.setConLai(tongTien - soTienCoc);

        // Tách ghi chú người dùng ra khỏi audit log
        print.setGhiChu(extractUserNote(donHang.getGhiChu()));

        // Khách hàng
        if (donHang.getKhachHang() != null) {
            NguoiDung kh = donHang.getKhachHang();
            print.setTenKhachHang(kh.getHoTen());
            print.setEmailKhachHang(kh.getEmail());
            print.setSdtKhachHang(kh.getSoDienThoai());
        }
        print.setDiaChiGiaoHang(donHang.getDiaChiGiao());

        // Nhân viên
        if (donHang.getNhanVien() != null) {
            print.setTenNhanVien(donHang.getNhanVien().getHoTen());
        }

        // Sản phẩm
        if (donHang.getChiTietDonHangs() != null) {
            List<OrderDto.PrintResponse.PrintItem> items = donHang.getChiTietDonHangs()
                    .stream().map(ct -> {
                        OrderDto.PrintResponse.PrintItem item = new OrderDto.PrintResponse.PrintItem();
                        item.setTenSanPham(ct.getSanPham().getTenSanPham());
                        item.setSoLuong(ct.getSoLuong());
                        double donGia = ct.getDonGiaTaiThoiDiem() != null
                                ? ct.getDonGiaTaiThoiDiem().doubleValue() : 0.0;
                        item.setDonGia(donGia);
                        item.setThanhTien(donGia * ct.getSoLuong());
                        return item;
                    }).collect(Collectors.toList());
            print.setItems(items);
        }

        return print;
    }

    // ── HELPERS ─────────────────────────────────────────────────────────────────

    private DonHang findOrder(Long id) {
        return donHangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng #" + id));
    }

    private NguoiDung findAdmin(String email) {
        return nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Lỗi xác thực Admin"));
    }

    private void appendMiniAuditLog(DonHang donHang, String tenAdmin, String action) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String logLine = String.format("\n[AUDIT %s - Admin %s]: %s", time, tenAdmin, action);
        String current = donHang.getGhiChu() == null ? "" : donHang.getGhiChu();
        donHang.setGhiChu(current + logLine);
    }

    /** Tách phần ghi chú người dùng (trước dòng AUDIT đầu tiên) */
    private String extractUserNote(String ghiChu) {
        if (ghiChu == null) return "";
        int auditIdx = ghiChu.indexOf("\n[AUDIT");
        return auditIdx >= 0 ? ghiChu.substring(0, auditIdx).trim() : ghiChu.trim();
    }

    /** Tách phần audit log (từ dòng AUDIT đầu tiên trở đi) */
    private String extractAuditLog(String ghiChu) {
        if (ghiChu == null) return "";
        int auditIdx = ghiChu.indexOf("\n[AUDIT");
        return auditIdx >= 0 ? ghiChu.substring(auditIdx).trim() : "";
    }

    private void notifyUser(DonHang donHang, String message) {
        if (donHang.getKhachHang() != null
                && !"khachvanglai@gmail.com".equals(donHang.getKhachHang().getEmail())) {
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
        if (donHang.getTongTien() != null) dto.setTongTien(donHang.getTongTien().doubleValue());
        dto.setTrangThai(donHang.getTrangThai());
        dto.setGhiChu(donHang.getGhiChu());
        dto.setLyDoHuy(donHang.getLyDoHuy());
        if (donHang.getNhanVien() != null) dto.setTenNhanVienPhuTrach(donHang.getNhanVien().getHoTen());

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