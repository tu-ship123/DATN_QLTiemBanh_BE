package com.poly.cake.service;

import com.poly.cake.entity.ChiTietDonHang;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.PhieuKiemKe;
import com.poly.cake.entity.SanPham;
import com.poly.cake.entity.ThongBao;
import com.poly.cake.exception.NgoaiLeNghiepVu;
import com.poly.cake.exception.NgoaiLeKhongTimThayTaiNguyen;
import com.poly.cake.repository.ChiTietDonHangRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.PhieuKiemKeRepository;
import com.poly.cake.repository.SanPhamRepository;
import com.poly.cake.repository.ThongBaoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class TonKhoService {

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private ThongBaoRepository thongBaoRepository;

    @Autowired
    private ThongBaoService notificationService;

    @Autowired

    private DichVuEmail emailService;

    private PhieuKiemKeRepository phieuKiemKeRepository; // Bổ sung Repository


    private static final List<String> NGUOI_NHAN_CANH_BAO = List.of("ADMIN", "NHAN_VIEN");

    /**
     * BỔ SUNG: Điều chỉnh số lượng tồn kho thủ công từ trang quản trị (Admin).
     * soLuongThayDoi > 0: Nhập thêm hàng.
     * soLuongThayDoi < 0: Xuất kho / điều chỉnh giảm.
     * Sau khi cập nhật, tự động kiểm tra ngưỡng để phát cảnh báo.
     */
    @Transactional
public SanPham dieuChinhTonKhoThuCong(Long id, Integer soLuongThayDoi, String lyDo, String nguoiThucHien) {
    if (soLuongThayDoi == null || soLuongThayDoi == 0) {
        throw new NgoaiLeNghiepVu("Số lượng thay đổi phải khác 0");
    }

    SanPham sanPham = sanPhamRepository.findById(id)
            .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen("Không tìm thấy sản phẩm với ID: " + id));

    int tonHeThongBanDau = sanPham.getSoLuongTon() != null ? sanPham.getSoLuongTon() : 0;

    if (soLuongThayDoi > 0) {
        sanPhamRepository.congLaiSoLuongTon(id, soLuongThayDoi);
    } else {
        int soDongBiAnhHuong = sanPhamRepository.truSoLuongTon(id, -soLuongThayDoi);
        if (soDongBiAnhHuong == 0) {
            throw new NgoaiLeNghiepVu("Số lượng tồn kho hiện tại không đủ để trừ " + (-soLuongThayDoi));
        }
    }

    // Lấy dữ liệu sản phẩm mới nhất sau khi update
    SanPham sanPhamMoiNhat = sanPhamRepository.findById(id)
            .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen("Không tìm thấy sản phẩm sau khi cập nhật"));

    int tonThucTeMoi = sanPhamMoiNhat.getSoLuongTon();

    // 🔴 GHI LẠI BẢN GHI LỊCH SỬ CHÊNH LỆCH KIỂM KÊ
    PhieuKiemKe pkk = PhieuKiemKe.builder()
            .sanPham(sanPhamMoiNhat)
            .tonHeThong(tonHeThongBanDau)
            .tonThucTe(tonThucTeMoi)
            .chenhLech(soLuongThayDoi) // dương: thừa/nhập, âm: thiếu/xuất
            .lyDo(lyDo != null ? lyDo : "Điều chỉnh kiểm kê tồn kho")
            .nguoiThucHien(nguoiThucHien)
            .build();
    phieuKiemKeRepository.save(pkk);

    // Kiểm tra xem mức tồn kho mới có chạm ngưỡng cảnh báo không
    kiemTraVaCanhBaoNeuTonKhoThap(sanPhamMoiNhat);

    return sanPhamMoiNhat;
}

    /**
     * Trừ tồn kho cho toàn bộ sản phẩm trong 1 đơn hàng (gọi khi thanh toán THÀNH CÔNG).
     * - Dùng UPDATE có điều kiện (truSoLuongTon) để tránh trừ âm khi có nhiều giao dịch đồng thời.
     * - KHÔNG throw exception khi thiếu hàng (vì tiền đã về tài khoản) — thay vào đó
     * gửi cảnh báo khẩn tới Admin/NhanVien để họ xử lý thủ công (liên hệ khách, bổ sung hàng...).
     * - Sau khi trừ thành công, nếu tồn kho <= ngưỡng cảnh báo -> gửi thông báo TON_KHO.
     */
    @Transactional
    public void truTonKhoTheoDonHang(DonHang donHang) {
        List<ChiTietDonHang> danhSachChiTiet = chiTietDonHangRepository.findByDonHang(donHang);

        if (danhSachChiTiet.isEmpty()) {
            log.warn("Đơn hàng HD-{} không có chi tiết sản phẩm, bỏ qua trừ tồn kho.", donHang.getId());
            return;
        }

        for (ChiTietDonHang ct : danhSachChiTiet) {
            Long sanPhamId = ct.getSanPham().getId();
            int soLuong = ct.getSoLuong();

            int soDongBiAnhHuong = sanPhamRepository.truSoLuongTon(sanPhamId, soLuong);

            if (soDongBiAnhHuong == 0) {
                // Không đủ tồn kho để trừ -> cảnh báo khẩn, không rollback giao dịch thanh toán
                canhBaoBanVuotTonKho(donHang, ct);
                continue;
            }

            // Đọc lại giá trị mới nhất (đã clear cache nhờ clearAutomatically = true)
            sanPhamRepository.findById(sanPhamId).ifPresent(this::kiemTraVaCanhBaoNeuTonKhoThap);
        }
    }

    /**
     * Kiểm tra tồn kho hiện tại của 1 sản phẩm so với ngưỡng cảnh báo (nguongCanhBao,
     * mặc định = 10), nếu <= ngưỡng thì gửi cảnh báo TON_KHO cho Admin/NhanVien.
     * Public để AdminSanPhamService (API cập nhật tồn kho thủ công / nhập hàng) có
     * thể gọi lại sau mỗi lần điều chỉnh số lượng, không chỉ riêng lúc trừ theo đơn hàng.
     */
    @Transactional
    public void kiemTraVaCanhBaoNeuTonKhoThap(SanPham sp) {
        if (sp == null || sp.getSoLuongTon() == null) {
            return;
        }
        // Ngưỡng cảnh báo cố định = 10 nếu sản phẩm chưa được gán riêng
        int nguong = sp.getNguongCanhBao() != null ? sp.getNguongCanhBao() : 10;
        if (sp.getSoLuongTon() <= nguong) {
            guiCanhBaoTonKhoThap(sp);
        }
    }

    /**
     * Gửi thông báo TON_KHO cho tất cả ADMIN + NHAN_VIEN đang hoạt động.
     * Có chống trùng: không gửi lại nếu đã cảnh báo sản phẩm này trong vòng 6 giờ gần nhất.
     */
    private void guiCanhBaoTonKhoThap(SanPham sp) {
        List<NguoiDung> nguoiNhan = nguoiDungRepository.findByQuyenInAndTrangThai(
                NGUOI_NHAN_CANH_BAO, "HOAT_DONG"
        );

        int nguongHienThi = sp.getNguongCanhBao() != null ? sp.getNguongCanhBao() : 10;
        String tieuDe = "Cảnh báo tồn kho thấp: " + sp.getTenSanPham();
        String noiDung = String.format(
                "Sản phẩm \"%s\" chỉ còn %d (ngưỡng cảnh báo: %d). Vui lòng nhập thêm hàng.",
                sp.getTenSanPham(), sp.getSoLuongTon(), nguongHienThi
        );
        LocalDateTime chongTrungTu = LocalDateTime.now().minusHours(6);

        boolean coGuiMoi = false;

        for (NguoiDung nd : nguoiNhan) {
            boolean daGuiGanDay = thongBaoRepository.existsCanhBaoTrungGanDay(nd, tieuDe, chongTrungTu);
            if (daGuiGanDay) {
                continue;
            }

            ThongBao tb = new ThongBao();
            tb.setNguoiDung(nd);
            tb.setTieuDe(tieuDe);
            tb.setNoiDung(noiDung);
            tb.setLoaiThongBao("TON_KHO");
            tb.setDaDoc(false);
            thongBaoRepository.save(tb);
            coGuiMoi = true;

            // Gửi email cảnh báo cho từng người nhận (best-effort, không rollback nếu lỗi)
            if (nd.getEmail() != null && !nd.getEmail().isBlank()) {
                try {
                    emailService.sendLowStockWarningEmail(
                            nd.getEmail(), sp.getTenSanPham(), sp.getSoLuongTon(), nguongHienThi);
                } catch (Exception e) {
                    log.error("Gửi email cảnh báo tồn kho thấp cho {} thất bại: {}", nd.getEmail(), e.getMessage());
                }
            }
        }

        // Chỉ push real-time 1 lần khi thực sự có thông báo mới (tránh spam broadcast trùng lặp)
        if (coGuiMoi) {
            notificationService.notifyLowStockToAdmins(
                    "⚠️ " + tieuDe + " (còn " + sp.getSoLuongTon() + ")"
            );
        }

        log.info("Đã gửi cảnh báo tồn kho thấp cho sản phẩm \"{}\" (còn {})",
                sp.getTenSanPham(), sp.getSoLuongTon());
    }

    /**
     * Trường hợp đặc biệt: khách thanh toán thành công nhưng tồn kho không đủ để trừ
     * (VD: 2 đơn cùng đặt sản phẩm cuối cùng). Cảnh báo khẩn cho Admin/NhanVien xử lý tay.
     */
    private void canhBaoBanVuotTonKho(DonHang donHang, ChiTietDonHang ct) {
        List<NguoiDung> nguoiNhan = nguoiDungRepository.findByQuyenInAndTrangThai(
                NGUOI_NHAN_CANH_BAO, "HOAT_DONG"
        );

        String tieuDe = "⚠️ KHẨN: Bán vượt tồn kho - HD-" + donHang.getId();
        String noiDung = String.format(
                "Đơn hàng HD-%d đã thanh toán nhưng sản phẩm \"%s\" không đủ tồn kho để trừ (cần %d). " +
                        "Vui lòng kiểm tra và liên hệ khách hàng nếu cần.",
                donHang.getId(), ct.getSanPham().getTenSanPham(), ct.getSoLuong()
        );

        for (NguoiDung nd : nguoiNhan) {
            ThongBao tb = new ThongBao();
            tb.setNguoiDung(nd);
            tb.setTieuDe(tieuDe);
            tb.setNoiDung(noiDung);
            tb.setLoaiThongBao("HE_THONG");
            tb.setDaDoc(false);
            thongBaoRepository.save(tb);
        }

        // Khẩn cấp -> luôn push real-time để admin xử lý ngay
        notificationService.notifyLowStockToAdmins(tieuDe);

        log.error("Bán vượt tồn kho: HD-{} - sản phẩm \"{}\" thiếu hàng!",
                donHang.getId(), ct.getSanPham().getTenSanPham());
    }
}