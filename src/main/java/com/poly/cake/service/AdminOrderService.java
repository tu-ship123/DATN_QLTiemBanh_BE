package com.poly.cake.service;

import com.poly.cake.exception.BusinessException;
import com.poly.cake.exception.ResourceNotFoundException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.cake.dto.OrderDto;
import com.poly.cake.entity.*;
import com.poly.cake.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final DonHangRepository donHangRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final SanPhamRepository sanPhamRepository;
    private final NotificationService notificationService;
    private final PhuKienTrangTriRepository phuKienTrangTriRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Dùng Enum thay vì String để đảm bảo Compile-time safety
    private static final List<TrangThaiDonHang> STATUS_FLOW = List.of(
            TrangThaiDonHang.CHO_XAC_NHAN, TrangThaiDonHang.DA_XAC_NHAN,
            TrangThaiDonHang.DANG_LAM, TrangThaiDonHang.SAN_SANG,
            TrangThaiDonHang.DANG_GIAO, TrangThaiDonHang.HOAN_THANH
    );

    // Dùng EnumSet để tối ưu hóa hiệu suất tìm kiếm
    private static final EnumSet<TrangThaiDonHang> TERMINAL_STATUSES = EnumSet.of(
            TrangThaiDonHang.HOAN_THANH, TrangThaiDonHang.DA_HUY, TrangThaiDonHang.DA_HOAN_TIEN
    );

    public List<OrderDto.Response> getFilteredOrders(String trangThai, String nguonDon, LocalDateTime tuNgay, LocalDateTime denNgay) {
        return donHangRepository.filterAdminOrders(trangThai, nguonDon, tuNgay, denNgay)
                .stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    @Transactional
    public OrderDto.Response overrideOrderStatus(Long id, String trangThaiMoi, String lyDo, String emailAdmin) {
        DonHang donHang = findOrder(id);
        NguoiDung admin = findAdmin(emailAdmin);

        TrangThaiDonHang trangThaiCu = donHang.getTrangThai();
        try {
            TrangThaiDonHang newStatus = TrangThaiDonHang.valueOf(trangThaiMoi.toUpperCase());
            donHang.setTrangThai(newStatus);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Trạng thái đơn hàng không hợp lệ: " + trangThaiMoi);
        }

        appendMiniAuditLog(donHang, admin.getHoTen(), "Ép đổi trạng thái từ " + trangThaiCu.name() + " sang " + trangThaiMoi + ". Lý do: " + lyDo);

        DonHang updatedDonHang = donHangRepository.save(donHang);
        notifyUser(updatedDonHang, "Đơn hàng HD-" + id + " của bạn đã được Admin xử lý: " + trangThaiMoi);

        return mapToResponseDto(updatedDonHang);
    }

    @Transactional
    public OrderDto.Response refundOrder(Long id, String lyDo, String emailAdmin) {
        DonHang donHang = findOrder(id);
        NguoiDung admin = findAdmin(emailAdmin);

        donHang.setTrangThai(TrangThaiDonHang.DA_HOAN_TIEN);
        donHang.setLyDoHuy("Hoàn tiền: " + lyDo);

        appendMiniAuditLog(donHang, admin.getHoTen(), "Hoàn tiền cho khách. Lý do: " + lyDo);

        DonHang updatedDonHang = donHangRepository.save(donHang);
        notifyUser(updatedDonHang, "Đơn hàng HD-" + id + " đã được hoàn tiền thành công. Lý do: " + lyDo);

        return mapToResponseDto(updatedDonHang);
    }

    @Transactional
    public void cancelAndRollbackInventory(Long id, String lyDo, String emailAdmin) {
        DonHang donHang = findOrder(id);
        NguoiDung admin = findAdmin(emailAdmin);

        if (donHang.getTrangThai() == TrangThaiDonHang.DA_HUY) {
            throw new BusinessException("Đơn hàng này đã bị hủy từ trước!");
        }

        donHang.setTrangThai(TrangThaiDonHang.DA_HUY);
        donHang.setLyDoHuy("Admin Hủy & Rollback kho: " + lyDo);
        appendMiniAuditLog(donHang, admin.getHoTen(), "Hủy đơn ép buộc & Hoàn số lượng về kho. Lý do: " + lyDo);

        DonHang updatedDonHang = donHangRepository.save(donHang);

        for (ChiTietDonHang ct : updatedDonHang.getChiTietDonHangs()) {
            sanPhamRepository.congLaiSoLuongTon(ct.getSanPham().getId(), ct.getSoLuong());
        }
        notifyUser(updatedDonHang, "Đơn hàng HD-" + id + " đã bị hủy bởi hệ thống. Lý do: " + lyDo);
    }

    @Transactional
    public OrderDto.Response updateOrderInfo(Long id, OrderDto.UpdateRequest request, String emailAdmin) {
        DonHang donHang = findOrder(id);
        NguoiDung admin = findAdmin(emailAdmin);

        if (TERMINAL_STATUSES.contains(donHang.getTrangThai())) {
            throw new BusinessException("Không thể chỉnh sửa đơn hàng ở trạng thái: " + donHang.getTrangThai().name());
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
            String auditPart = extractAuditLog(donHang.getGhiChu());
            donHang.setGhiChu(request.getGhiChu() + (auditPart.isEmpty() ? "" : "\n" + auditPart));
            changes.append(" GhiChú=[đã cập nhật]");
            hasChange = true;
        }

        if (!hasChange) {
            throw new BusinessException("Không có thông tin nào được thay đổi!");
        }

        appendMiniAuditLog(donHang, admin.getHoTen(), changes.toString());
        DonHang saved = donHangRepository.save(donHang);
        return mapToResponseDto(saved);
    }

    @Transactional
    public OrderDto.Response changeOrderStatus(Long id, String trangThaiMoi, String lyDoHuy, String emailAdmin) {
        DonHang donHang = findOrder(id);
        NguoiDung admin = findAdmin(emailAdmin);

        TrangThaiDonHang trangThaiHienTai = donHang.getTrangThai();
        TrangThaiDonHang newStatus;

        try {
            newStatus = TrangThaiDonHang.valueOf(trangThaiMoi.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Trạng thái '" + trangThaiMoi + "' không hợp lệ!");
        }

        if (TERMINAL_STATUSES.contains(trangThaiHienTai)) {
            throw new BusinessException("Đơn hàng đã kết thúc, không thể đổi trạng thái!");
        }

        if (newStatus != TrangThaiDonHang.DA_HUY) {
            int currentIdx = STATUS_FLOW.indexOf(trangThaiHienTai);
            int newIdx     = STATUS_FLOW.indexOf(newStatus);
            if (newIdx < 0) {
                throw new BusinessException("Trạng thái không nằm trong luồng chuẩn!");
            }
            if (newIdx <= currentIdx) {
                throw new BusinessException("Không thể lùi trạng thái! Hiện tại: " + trangThaiHienTai.name());
            }
        } else {
            if (lyDoHuy == null || lyDoHuy.trim().isEmpty()) {
                throw new BusinessException("Bắt buộc phải nhập lý do khi hủy đơn hàng!");
            }
            donHang.setLyDoHuy(lyDoHuy);
        }

        donHang.setTrangThai(newStatus);
        donHang.setNhanVien(admin);
        appendMiniAuditLog(donHang, admin.getHoTen(), "Đổi trạng thái → " + newStatus.name());

        DonHang saved = donHangRepository.save(donHang);
        notifyUser(saved, "Đơn hàng HD-" + id + " của bạn vừa chuyển sang: " + newStatus.name());

        return mapToResponseDto(saved);
    }

    public OrderDto.PrintResponse getPrintData(Long id) {
        DonHang donHang = findOrder(id);

        OrderDto.PrintResponse print = new OrderDto.PrintResponse();
        print.setId(donHang.getId());
        print.setMaDonHang("HD-" + donHang.getId());
        print.setTrangThai(donHang.getTrangThai().name());
        print.setNgayTao(donHang.getNgayTao());
        print.setNguonDon(donHang.getNguonDon());

        if (donHang.getNgayGiaoDuKien() != null) {
            print.setNgayGiaoHang(donHang.getNgayGiaoDuKien().toLocalDate());
        }

        double tongTien  = donHang.getTongTien()  != null ? donHang.getTongTien().doubleValue()  : 0.0;
        double soTienCoc = donHang.getSoTienCoc() != null ? donHang.getSoTienCoc().doubleValue() : 0.0;
        print.setTongTien(tongTien);
        print.setSoTienCoc(soTienCoc);
        print.setConLai(tongTien - soTienCoc);
        print.setGhiChu(extractUserNote(donHang.getGhiChu()));

        if (donHang.getKhachHang() != null) {
            NguoiDung kh = donHang.getKhachHang();
            print.setTenKhachHang(kh.getHoTen());
            print.setEmailKhachHang(kh.getEmail());
            print.setSdtKhachHang(kh.getSoDienThoai());
        }
        print.setDiaChiGiaoHang(donHang.getDiaChiGiao());

        if (donHang.getNhanVien() != null) {
            print.setTenNhanVien(donHang.getNhanVien().getHoTen());
        }

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

    @Transactional
    public OrderDto.Response createOrder(OrderDto.Request request, String emailNhanVien) {
        NguoiDung nhanVien = nguoiDungRepository.findByEmail(emailNhanVien)
                .orElseThrow(() -> new ResourceNotFoundException("Lỗi xác thực Nhân viên/Admin"));

        DonHang donHang = new DonHang();
        donHang.setDiaChiGiao(request.getDiaChiGiaoHang());

        if (request.getNgayGiaoHang() != null) {
            donHang.setNgayGiaoDuKien(request.getNgayGiaoHang().atStartOfDay());
        }

        donHang.setGhiChu(request.getGhiChu() != null ? request.getGhiChu() : "");
        donHang.setNgayTao(LocalDateTime.now());
        donHang.setTrangThai(TrangThaiDonHang.DA_XAC_NHAN);
        donHang.setNhanVien(nhanVien);

        NguoiDung khachVangLai = nguoiDungRepository.findByEmail("khachvanglai@gmail.com")
                .orElseThrow(() -> new BusinessException("Lỗi: Trong Database chưa có user khachvanglai@gmail.com. Vui lòng tạo tài khoản này trước!"));
        donHang.setKhachHang(khachVangLai);

        if (request.getSoDienThoai() != null && !request.getSoDienThoai().isEmpty()) {
            donHang.setGhiChu(donHang.getGhiChu() + " | SĐT Khách: " + request.getSoDienThoai());
        }

        BigDecimal tongTien = BigDecimal.ZERO;
        List<ChiTietDonHang> chiTietList = new ArrayList<>();

        if (request.getItems() != null) {
            for (OrderDto.OrderItemRequest itemReq : request.getItems()) {
                SanPham sanPham = sanPhamRepository.findById(itemReq.getSanPhamId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm ID: " + itemReq.getSanPhamId()));

                if (sanPham.getSoLuongTon() < itemReq.getSoLuong()) {
                    throw new BusinessException("Sản phẩm '" + sanPham.getTenSanPham() + "' không đủ số lượng tồn kho!");
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

                tongTien = tongTien.add(donGia.multiply(BigDecimal.valueOf(itemReq.getSoLuong())));
            }
        }

        donHang.setTongTien(tongTien);
        donHang.setChiTietDonHangs(chiTietList);

        appendMiniAuditLog(donHang, nhanVien.getHoTen(), "Khởi tạo đơn hàng mới từ hệ thống Admin.");

        DonHang savedDonHang = donHangRepository.save(donHang);
        return mapToResponseDto(savedDonHang);
    }

    @Transactional
    public OrderDto.Response confirmDesign(Long orderId, String emailNhanVien) {
        DonHang donHang = findOrder(orderId);
        NguoiDung nhanVien = nguoiDungRepository.findByEmail(emailNhanVien)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin nhân viên!"));

        String designJson = donHang.getThietKeBanhJson();
        if (designJson == null || designJson.trim().isEmpty()) {
            throw new BusinessException("Đơn hàng HD-" + orderId + " không có thiết kế bánh 3D để xác nhận!");
        }

        if (donHang.getTrangThai() != TrangThaiDonHang.DA_XAC_NHAN) {
            throw new BusinessException("Chỉ có thể xác nhận thiết kế khi đơn ở trạng thái DA_XAC_NHAN!");
        }

        List<Map<String, Object>> danhSachPhuKien = parsePhuKienFromDesign(designJson, orderId);

        Map<Long, PhuKienTrangTri> phuKienCache = new java.util.LinkedHashMap<>();
        Map<Long, Integer> soLuongCanTru = new java.util.LinkedHashMap<>();

        for (Map<String, Object> pk : danhSachPhuKien) {
            Long phuKienId = toLong(pk.get("phu_kien_id"), "phu_kien_id");
            int soLuongCan = toInt(pk.get("so_luong"), "so_luong");

            PhuKienTrangTri phuKien = phuKienTrangTriRepository.findById(phuKienId)
                    .orElseThrow(() -> new ResourceNotFoundException("Phụ kiện trang trí ID=" + phuKienId + " không tồn tại!"));

            if (!phuKien.getHoatDong()) {
                throw new BusinessException("Phụ kiện '" + phuKien.getTenPhuKien() + "' đã ngừng kinh doanh!");
            }

            if (phuKien.getSoLuongTon() < soLuongCan) {
                throw new BusinessException("Phụ kiện '" + phuKien.getTenPhuKien() + "' không đủ tồn kho!");
            }

            phuKienCache.put(phuKienId, phuKien);
            soLuongCanTru.put(phuKienId, soLuongCan);
        }

        StringBuilder dsPhuKienLog = new StringBuilder();
        for (Map.Entry<Long, PhuKienTrangTri> entry : phuKienCache.entrySet()) {
            PhuKienTrangTri phuKien = entry.getValue();
            int soLuongCan = soLuongCanTru.get(entry.getKey());

            phuKien.setSoLuongTon(phuKien.getSoLuongTon() - soLuongCan);
            phuKienTrangTriRepository.save(phuKien);

            dsPhuKienLog.append(phuKien.getTenPhuKien()).append(" x").append(soLuongCan).append(", ");
        }

        donHang.setTrangThai(TrangThaiDonHang.DANG_LAM);
        donHang.setNhanVien(nhanVien);

        String logContent = "Xác nhận thiết kế 3D → chuyển sang DANG_LAM.";
        if (!danhSachPhuKien.isEmpty()) {
            String dsPk = dsPhuKienLog.toString();
            logContent += " Đã trừ kho phụ kiện: " + dsPk.substring(0, dsPk.length() - 2) + ".";
        }
        appendMiniAuditLog(donHang, nhanVien.getHoTen(), logContent);

        DonHang saved = donHangRepository.save(donHang);
        notifyUser(saved, "🎂 Đơn hàng HD-" + orderId + " của bạn đã được xác nhận thiết kế và đang được làm bánh!");

        return mapToResponseDto(saved);
    }

    @Transactional
    public OrderDto.Response rejectDesign(Long orderId, String lyDo, String emailNhanVien) {
        DonHang donHang = findOrder(orderId);
        NguoiDung nhanVien = nguoiDungRepository.findByEmail(emailNhanVien)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin nhân viên!"));

        if (lyDo == null || lyDo.trim().isEmpty()) {
            throw new BusinessException("Bắt buộc phải nhập lý do từ chối thiết kế!");
        }

        String designJson = donHang.getThietKeBanhJson();
        if (designJson == null || designJson.trim().isEmpty()) {
            throw new BusinessException("Đơn hàng HD-" + orderId + " không có thiết kế bánh 3D để từ chối!");
        }

        if (donHang.getTrangThai() != TrangThaiDonHang.DA_XAC_NHAN) {
            throw new BusinessException("Chỉ có thể từ chối thiết kế khi đơn ở trạng thái DA_XAC_NHAN!");
        }

        donHang.setTrangThai(TrangThaiDonHang.CHO_XAC_NHAN);
        donHang.setNhanVien(nhanVien);
        donHang.setLyDoHuy("Từ chối thiết kế: " + lyDo.trim());

        appendMiniAuditLog(donHang, nhanVien.getHoTen(), "Từ chối thiết kế 3D → quay về CHO_XAC_NHAN. Lý do: " + lyDo.trim());

        DonHang saved = donHangRepository.save(donHang);
        notifyUser(saved, "⚠️ Đơn hàng HD-" + orderId + " – Thiết kế bánh chưa được duyệt. Lý do: " + lyDo.trim());

        return mapToResponseDto(saved);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parsePhuKienFromDesign(String designJson, Long orderId) {
        try {
            Map<String, Object> designMap = objectMapper.readValue(designJson, new TypeReference<Map<String, Object>>() {});
            Object trangTriObj = designMap.get("trang_tri");
            if (trangTriObj == null) return new ArrayList<>();
            if (!(trangTriObj instanceof List)) throw new BusinessException("Trường 'trang_tri' phải là mảng!");

            List<Object> rawList = (List<Object>) trangTriObj;
            List<Map<String, Object>> result = new ArrayList<>();

            for (Object item : rawList) {
                if (!(item instanceof Map)) continue;
                Map<String, Object> pkMap = (Map<String, Object>) item;
                if (pkMap.get("phu_kien_id") == null) continue;
                if (pkMap.get("so_luong") == null) {
                    pkMap = new java.util.HashMap<>(pkMap);
                    pkMap.put("so_luong", 1);
                }
                result.add(pkMap);
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Lỗi khi đọc dữ liệu thiết kế 3D của đơn HD-" + orderId + ": " + e.getMessage());
        }
    }

    private DonHang findOrder(Long id) {
        return donHangRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + id));
    }

    private NguoiDung findAdmin(String email) {
        return nguoiDungRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Lỗi xác thực Admin"));
    }

    private void appendMiniAuditLog(DonHang donHang, String tenNhanVien, String action) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String logLine = String.format("\n[AUDIT %s - %s]: %s", time, tenNhanVien, action);
        String currentNote = (donHang.getGhiChu() == null) ? "" : donHang.getGhiChu();
        donHang.setGhiChu(currentNote + logLine);
    }

    private String extractUserNote(String ghiChu) {
        if (ghiChu == null) return "";
        int auditIdx = ghiChu.indexOf("\n[AUDIT");
        return auditIdx >= 0 ? ghiChu.substring(0, auditIdx).trim() : ghiChu.trim();
    }

    private String extractAuditLog(String ghiChu) {
        if (ghiChu == null) return "";
        int auditIdx = ghiChu.indexOf("\n[AUDIT");
        return auditIdx >= 0 ? ghiChu.substring(auditIdx).trim() : "";
    }

    private void notifyUser(DonHang donHang, String message) {
        if (donHang.getKhachHang() != null && !"khachvanglai@gmail.com".equals(donHang.getKhachHang().getEmail())) {
            notificationService.notifyOrderStatusToUser(donHang.getKhachHang().getEmail(), message);
        }
    }

    private Long toLong(Object value, String fieldName) {
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); }
        catch (Exception e) { throw new BusinessException("Trường '" + fieldName + "' phải là số nguyên!"); }
    }

    private int toInt(Object value, String fieldName) {
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); }
        catch (Exception e) { throw new BusinessException("Trường '" + fieldName + "' phải là số nguyên!"); }
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

        // Trả về tên Enum (String) cho Frontend
        dto.setTrangThai(donHang.getTrangThai().name());

        dto.setGhiChu(donHang.getGhiChu());
        dto.setLyDoHuy(donHang.getLyDoHuy());
        if (donHang.getNhanVien() != null) dto.setTenNhanVienPhuTrach(donHang.getNhanVien().getHoTen());
        dto.setCoThietKe3D(donHang.getThietKeBanhJson() != null && !donHang.getThietKeBanhJson().trim().isEmpty());

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