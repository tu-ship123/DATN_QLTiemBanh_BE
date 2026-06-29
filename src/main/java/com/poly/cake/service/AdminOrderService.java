package com.poly.cake.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.cake.dto.OrderDto;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.ChiTietDonHang;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.PhuKienTrangTri;
import com.poly.cake.entity.SanPham;
import com.poly.cake.repository.DonHangRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.PhuKienTrangTriRepository;
import com.poly.cake.repository.SanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    // T056 – Trừ tồn kho phụ kiện trang trí
    @Autowired
    private PhuKienTrangTriRepository phuKienTrangTriRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Thứ tự hợp lệ của các trạng thái (flow chuẩn)
    private static final List<String> STATUS_FLOW = List.of(
            "CHO_XAC_NHAN", "DA_XAC_NHAN", "DANG_LAM", "SAN_SANG", "DANG_GIAO", "HOAN_THANH"
    );
    // Trạng thái cuối, không được chuyển tiếp
    private static final Set<String> TERMINAL_STATUSES = Set.of("HOAN_THANH", "DA_HUY", "DA_HOAN_TIEN");

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. GET FILTER NÂNG CAO
    // ═══════════════════════════════════════════════════════════════════════════
    public List<OrderDto.Response> getFilteredOrders(String trangThai, String nguonDon, LocalDateTime tuNgay, LocalDateTime denNgay) {
        return donHangRepository.filterAdminOrders(trangThai, nguonDon, tuNgay, denNgay)
                .stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. PUT OVERRIDE STATUS + GHI MINI AUDIT LOG
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public OrderDto.Response overrideOrderStatus(Long id, String trangThaiMoi, String lyDo, String emailAdmin) {
        DonHang donHang = findOrder(id);
        NguoiDung admin = findAdmin(emailAdmin);

        String trangThaiCu = donHang.getTrangThai();
        donHang.setTrangThai(trangThaiMoi.toUpperCase());

        appendMiniAuditLog(donHang, admin.getHoTen(), "Ép đổi trạng thái từ " + trangThaiCu + " sang " + trangThaiMoi + ". Lý do: " + lyDo);

        DonHang updatedDonHang = donHangRepository.save(donHang);
        notifyUser(updatedDonHang, "Đơn hàng HD-" + id + " của bạn đã được Admin xử lý: " + trangThaiMoi);

        return mapToResponseDto(updatedDonHang);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3. POST REFUND (HOÀN TIỀN)
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public OrderDto.Response refundOrder(Long id, String lyDo, String emailAdmin) {
        DonHang donHang = findOrder(id);
        NguoiDung admin = findAdmin(emailAdmin);

        donHang.setTrangThai("DA_HOAN_TIEN");
        donHang.setLyDoHuy("Hoàn tiền: " + lyDo);

        appendMiniAuditLog(donHang, admin.getHoTen(), "Hoàn tiền cho khách. Lý do: " + lyDo);

        DonHang updatedDonHang = donHangRepository.save(donHang);
        notifyUser(updatedDonHang, "Đơn hàng HD-" + id + " đã được hoàn tiền thành công. Lý do: " + lyDo);

        return mapToResponseDto(updatedDonHang);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 4. DELETE CANCEL + ROLLBACK INVENTORY (Hoàn kho)
    // ═══════════════════════════════════════════════════════════════════════════
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

        DonHang updatedDonHang = donHangRepository.save(donHang);

        for (ChiTietDonHang ct : updatedDonHang.getChiTietDonHangs()) {
            SanPham sp = ct.getSanPham();
            sp.setSoLuongTon(sp.getSoLuongTon() + ct.getSoLuong());
            sanPhamRepository.save(sp);
        }

        notifyUser(updatedDonHang, "Đơn hàng HD-" + id + " đã bị hủy bởi hệ thống. Lý do: " + lyDo);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 5. PUT: Chỉnh sửa thông tin đơn hàng (địa chỉ, SĐT, ngày giao, ghi chú)
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public OrderDto.Response updateOrderInfo(Long id, OrderDto.UpdateRequest request, String emailAdmin) {
        DonHang donHang = findOrder(id);
        NguoiDung admin = findAdmin(emailAdmin);

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
            String auditPart = extractAuditLog(donHang.getGhiChu());
            donHang.setGhiChu(request.getGhiChu() + (auditPart.isEmpty() ? "" : "\n" + auditPart));
            changes.append(" GhiChú=[đã cập nhật]");
            hasChange = true;
        }

        if (!hasChange) {
            throw new RuntimeException("Không có thông tin nào được thay đổi!");
        }

        appendMiniAuditLog(donHang, admin.getHoTen(), changes.toString());
        DonHang saved = donHangRepository.save(donHang);
        return mapToResponseDto(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 6. PUT: Đổi trạng thái theo flow chuẩn (có validate thứ tự)
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public OrderDto.Response changeOrderStatus(Long id, String trangThaiMoi, String lyDoHuy, String emailAdmin) {
        DonHang donHang = findOrder(id);
        NguoiDung admin = findAdmin(emailAdmin);

        String trangThaiHienTai = donHang.getTrangThai();
        String trangThaiMoiUpper = trangThaiMoi.toUpperCase();

        if (TERMINAL_STATUSES.contains(trangThaiHienTai)) {
            throw new RuntimeException("Đơn hàng đã kết thúc, không thể đổi trạng thái!");
        }

        if (!"DA_HUY".equals(trangThaiMoiUpper)) {
            int currentIdx = STATUS_FLOW.indexOf(trangThaiHienTai);
            int newIdx     = STATUS_FLOW.indexOf(trangThaiMoiUpper);
            if (newIdx < 0) {
                throw new RuntimeException("Trạng thái '" + trangThaiMoi + "' không hợp lệ!");
            }
            if (newIdx <= currentIdx) {
                throw new RuntimeException("Không thể lùi trạng thái! Hiện tại: " + trangThaiHienTai);
            }
        } else {
            if (lyDoHuy == null || lyDoHuy.trim().isEmpty()) {
                throw new RuntimeException("Bắt buộc phải nhập lý do khi hủy đơn hàng!");
            }
            donHang.setLyDoHuy(lyDoHuy);
        }

        donHang.setTrangThai(trangThaiMoiUpper);
        donHang.setNhanVien(admin);
        appendMiniAuditLog(donHang, admin.getHoTen(), "Đổi trạng thái → " + trangThaiMoiUpper);

        DonHang saved = donHangRepository.save(donHang);
        notifyUser(saved, "Đơn hàng HD-" + id + " của bạn vừa chuyển sang: " + trangThaiMoiUpper);

        return mapToResponseDto(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 7. GET: Dữ liệu in đơn đầy đủ
    // ═══════════════════════════════════════════════════════════════════════════
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

    // ═══════════════════════════════════════════════════════════════════════════
    // 8. POST: Tạo đơn hàng mới từ Admin/Staff
    // ═══════════════════════════════════════════════════════════════════════════
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

                tongTien = tongTien.add(donGia.multiply(BigDecimal.valueOf(itemReq.getSoLuong())));
            }
        }

        donHang.setTongTien(tongTien);
        donHang.setChiTietDonHangs(chiTietList);

        appendMiniAuditLog(donHang, nhanVien.getHoTen(), "Khởi tạo đơn hàng mới từ hệ thống Admin.");

        DonHang savedDonHang = donHangRepository.save(donHang);
        return mapToResponseDto(savedDonHang);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // T056 – XÁC NHẬN THIẾT KẾ (confirm-design)
    //
    // Luồng:
    //   Nhân viên nhấn "Xác nhận thiết kế" trên đơn có bánh 3D
    //   → Kiểm tra đơn hợp lệ (có thiết kế, đúng trạng thái)
    //   → Parse JSON lấy danh sách phụ kiện trang trí
    //   → Kiểm tra & trừ tồn kho từng phụ kiện (transaction an toàn)
    //   → Chuyển đơn sang DANG_LAM
    //   → Ghi audit log
    //   → Thông báo khách
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public OrderDto.Response confirmDesign(Long orderId, String emailNhanVien) {
        // 1. Tìm đơn hàng
        DonHang donHang = findOrder(orderId);
        NguoiDung nhanVien = nguoiDungRepository.findByEmail(emailNhanVien)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên!"));

        // 2. Kiểm tra đơn phải có thiết kế 3D
        String designJson = donHang.getThietKeBanhJson();
        if (designJson == null || designJson.trim().isEmpty()) {
            throw new RuntimeException("Đơn hàng HD-" + orderId + " không có thiết kế bánh 3D để xác nhận!");
        }

        // 3. Kiểm tra trạng thái hợp lệ: chỉ xác nhận khi đơn đang ở DA_XAC_NHAN
        String trangThaiHienTai = donHang.getTrangThai();
        if (!"DA_XAC_NHAN".equals(trangThaiHienTai)) {
            throw new RuntimeException(
                    "Chỉ có thể xác nhận thiết kế khi đơn ở trạng thái DA_XAC_NHAN! " +
                    "Trạng thái hiện tại: " + trangThaiHienTai);
        }

        // 4. Parse JSON thiết kế → lấy danh sách phụ kiện trang trí
        List<Map<String, Object>> danhSachPhuKien = parsePhuKienFromDesign(designJson, orderId);

        // 5. Kiểm tra tồn kho TRƯỚC khi trừ + cache entity để dùng lại ở bước 6
        //    (tránh query DB 2 lần và tránh .get() không an toàn)
        Map<Long, PhuKienTrangTri> phuKienCache = new java.util.LinkedHashMap<>();
        Map<Long, Integer> soLuongCanTru = new java.util.LinkedHashMap<>();

        for (Map<String, Object> pk : danhSachPhuKien) {
            Long phuKienId = toLong(pk.get("phu_kien_id"), "phu_kien_id");
            int soLuongCan = toInt(pk.get("so_luong"), "so_luong");

            PhuKienTrangTri phuKien = phuKienTrangTriRepository.findById(phuKienId)
                    .orElseThrow(() -> new RuntimeException(
                            "Phụ kiện trang trí ID=" + phuKienId + " không tồn tại trong hệ thống!"));

            if (!phuKien.getHoatDong()) {
                throw new RuntimeException(
                        "Phụ kiện '" + phuKien.getTenPhuKien() + "' đã ngừng kinh doanh, không thể dùng cho đơn này!");
            }

            if (phuKien.getSoLuongTon() < soLuongCan) {
                throw new RuntimeException(
                        "Phụ kiện '" + phuKien.getTenPhuKien() + "' không đủ tồn kho! " +
                        "Cần: " + soLuongCan + ", Còn: " + phuKien.getSoLuongTon());
            }

            phuKienCache.put(phuKienId, phuKien);
            soLuongCanTru.put(phuKienId, soLuongCan);
        }

        // 6. Trừ tồn kho từng phụ kiện (dùng lại cache từ bước 5, không query lại DB)
        StringBuilder dsPhuKienLog = new StringBuilder();
        for (Map.Entry<Long, PhuKienTrangTri> entry : phuKienCache.entrySet()) {
            PhuKienTrangTri phuKien = entry.getValue();
            int soLuongCan = soLuongCanTru.get(entry.getKey());

            phuKien.setSoLuongTon(phuKien.getSoLuongTon() - soLuongCan);
            phuKienTrangTriRepository.save(phuKien);

            dsPhuKienLog.append(phuKien.getTenPhuKien()).append(" x").append(soLuongCan).append(", ");
        }

        // 7. Chuyển trạng thái đơn → DANG_LAM
        donHang.setTrangThai("DANG_LAM");
        donHang.setNhanVien(nhanVien);

        // 8. Ghi audit log
        String logContent = "Xác nhận thiết kế 3D → chuyển sang DANG_LAM.";
        if (danhSachPhuKien.isEmpty()) {
            logContent += " Không có phụ kiện trang trí cần trừ kho.";
        } else {
            String dsPk = dsPhuKienLog.toString();
            logContent += " Đã trừ kho phụ kiện: " + dsPk.substring(0, dsPk.length() - 2) + ".";
        }
        appendMiniAuditLog(donHang, nhanVien.getHoTen(), logContent);

        DonHang saved = donHangRepository.save(donHang);

        // 9. Thông báo khách
        notifyUser(saved,
                "🎂 Đơn hàng HD-" + orderId + " của bạn đã được xác nhận thiết kế và đang được làm bánh!");

        return mapToResponseDto(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // T057 – TỪ CHỐI THIẾT KẾ (reject-design)
    //
    // Luồng:
    //   Nhân viên nhấn "Từ chối thiết kế" + nhập lý do
    //   → Kiểm tra đơn có thiết kế 3D và đang ở DA_XAC_NHAN
    //   → Chuyển đơn về CHO_XAC_NHAN (khách sửa lại thiết kế)
    //   → Lưu lý do vào lyDoHuy (tái sử dụng field)
    //   → Ghi audit log
    //   → Thông báo khách cần chỉnh sửa lại thiết kế
    // ═══════════════════════════════════════════════════════════════════════════
    @Transactional
    public OrderDto.Response rejectDesign(Long orderId, String lyDo, String emailNhanVien) {
        // 1. Tìm đơn hàng
        DonHang donHang = findOrder(orderId);
        NguoiDung nhanVien = nguoiDungRepository.findByEmail(emailNhanVien)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên!"));

        // 2. Kiểm tra bắt buộc nhập lý do
        if (lyDo == null || lyDo.trim().isEmpty()) {
            throw new RuntimeException("Bắt buộc phải nhập lý do từ chối thiết kế!");
        }

        // 3. Kiểm tra đơn phải có thiết kế 3D
        String designJson = donHang.getThietKeBanhJson();
        if (designJson == null || designJson.trim().isEmpty()) {
            throw new RuntimeException("Đơn hàng HD-" + orderId + " không có thiết kế bánh 3D để từ chối!");
        }

        // 4. Chỉ từ chối khi đơn đang ở DA_XAC_NHAN
        String trangThaiHienTai = donHang.getTrangThai();
        if (!"DA_XAC_NHAN".equals(trangThaiHienTai)) {
            throw new RuntimeException(
                    "Chỉ có thể từ chối thiết kế khi đơn ở trạng thái DA_XAC_NHAN! " +
                    "Trạng thái hiện tại: " + trangThaiHienTai);
        }

        // 5. Quay đơn về CHO_XAC_NHAN để khách sửa lại thiết kế
        donHang.setTrangThai("CHO_XAC_NHAN");
        donHang.setNhanVien(nhanVien);
        donHang.setLyDoHuy("Từ chối thiết kế: " + lyDo.trim());

        // 6. Ghi audit log
        appendMiniAuditLog(donHang, nhanVien.getHoTen(),
                "Từ chối thiết kế 3D → quay về CHO_XAC_NHAN. Lý do: " + lyDo.trim());

        DonHang saved = donHangRepository.save(donHang);

        // 7. Thông báo khách cần chỉnh sửa lại thiết kế
        notifyUser(saved,
                "⚠️ Đơn hàng HD-" + orderId + " – Thiết kế bánh của bạn chưa được duyệt. " +
                "Lý do: " + lyDo.trim() + ". Vui lòng cập nhật lại thiết kế và đặt lại.");

        return mapToResponseDto(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HÀM PHỤ TRỢ T056 – Parse phụ kiện từ JSON thiết kế
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Parse mảng "trang_tri" từ JSON thiết kế.
     * Cấu trúc FE gửi lên:
     * {
     *   "khung": {...},
     *   "tang": [...],
     *   "trang_tri": [
     *     { "phu_kien_id": 1, "so_luong": 2, "ten": "Nến sinh nhật" },
     *     { "phu_kien_id": 3, "so_luong": 1, "ten": "Hoa kem" }
     *   ]
     * }
     * Nếu không có mảng trang_tri hoặc rỗng → trả về list rỗng (đơn không cần trừ phụ kiện)
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parsePhuKienFromDesign(String designJson, Long orderId) {
        try {
            Map<String, Object> designMap = objectMapper.readValue(
                    designJson, new TypeReference<Map<String, Object>>() {});

            Object trangTriObj = designMap.get("trang_tri");
            if (trangTriObj == null) {
                return new ArrayList<>();
            }
            if (!(trangTriObj instanceof List)) {
                throw new RuntimeException("Trường 'trang_tri' trong thiết kế 3D không đúng cấu trúc (phải là mảng)!");
            }

            List<Object> rawList = (List<Object>) trangTriObj;
            List<Map<String, Object>> result = new ArrayList<>();

            for (Object item : rawList) {
                if (!(item instanceof Map)) continue;
                Map<String, Object> pkMap = (Map<String, Object>) item;

                // Bỏ qua entry không có phu_kien_id (ví dụ trang trí chỉ là text/màu sắc)
                if (pkMap.get("phu_kien_id") == null) continue;

                // so_luong mặc định là 1 nếu FE không gửi
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
            throw new RuntimeException(
                    "Lỗi khi đọc dữ liệu thiết kế 3D của đơn HD-" + orderId + ": " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CÁC HÀM PHỤ TRỢ CHUNG
    // ═══════════════════════════════════════════════════════════════════════════

    private DonHang findOrder(Long id) {
        return donHangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng #" + id));
    }

    private NguoiDung findAdmin(String email) {
        return nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Lỗi xác thực Admin"));
    }

    private void appendMiniAuditLog(DonHang donHang, String tenNhanVien, String action) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String logLine = String.format("\n[AUDIT %s - %s]: %s", time, tenNhanVien, action);
        String currentNote = (donHang.getGhiChu() == null) ? "" : donHang.getGhiChu();
        donHang.setGhiChu(currentNote + logLine);
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
        if (donHang.getKhachHang() != null &&
                !"khachvanglai@gmail.com".equals(donHang.getKhachHang().getEmail())) {
            notificationService.notifyOrderStatusToUser(donHang.getKhachHang().getEmail(), message);
        }
    }

    private Long toLong(Object value, String fieldName) {
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); }
        catch (Exception e) {
            throw new RuntimeException("Trường '" + fieldName + "' trong trang_tri phải là số nguyên!");
        }
    }

    private int toInt(Object value, String fieldName) {
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); }
        catch (Exception e) {
            throw new RuntimeException("Trường '" + fieldName + "' trong trang_tri phải là số nguyên!");
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
        if (donHang.getNhanVien() != null) {
            dto.setTenNhanVienPhuTrach(donHang.getNhanVien().getHoTen());
        }
        // T055 – coThietKe3D
        dto.setCoThietKe3D(donHang.getThietKeBanhJson() != null &&
                !donHang.getThietKeBanhJson().trim().isEmpty());

        if (donHang.getChiTietDonHangs() != null) {
            List<OrderDto.OrderItemResponse> itemDtos = donHang.getChiTietDonHangs().stream().map(item -> {
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