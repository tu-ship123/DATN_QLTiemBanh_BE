package com.poly.cake.service;

import com.poly.cake.dto.PosOrderDto;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PosOrderService {

    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Transactional
    public PosOrderDto.Response createPosOrder(PosOrderDto.Request request, String emailNhanVien) {
        // 1. Xác định nhân viên đang đứng quầy chốt đơn
        NguoiDung nhanVien = nguoiDungRepository.findByEmail(emailNhanVien)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên!"));

        // 2. Xác định khách hàng (Nếu trống thì dùng email của nhân viên đang xử lý đơn)
        String emailKhach = (request.getEmailKhachHang() == null || request.getEmailKhachHang().isBlank())
                ? emailNhanVien : request.getEmailKhachHang();

        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailKhach)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản khách hàng với email: " + emailKhach));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Hóa đơn phải có ít nhất 1 sản phẩm!");
        }

        // 3. Khởi tạo đơn hàng POS
        DonHang donHang = new DonHang();
        donHang.setKhachHang(khachHang);
        donHang.setNhanVien(nhanVien);
        donHang.setNguonDon("POS");
        donHang.setTrangThai("DA_XAC_NHAN"); // Mua tại quầy thì mặc định xác nhận luôn
        donHang.setNgayTao(LocalDateTime.now());
        donHang.setGhiChu(request.getGhiChu());
        donHang.setTongTien(BigDecimal.ZERO); // Tạm thời gán bằng 0 để cộng dồn sau

        DonHang savedDonHang = donHangRepository.save(donHang);

        // 4. Duyệt danh sách món, cộng dồn tiền hàng, trừ tồn kho và lưu chi tiết
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ChiTietDonHang> chiTietList = new ArrayList<>();
        StringBuilder receiptItems = new StringBuilder(); // Dùng để build văn bản in hóa đơn

        for (PosOrderDto.ItemRequest item : request.getItems()) {
            SanPham sanPham = sanPhamRepository.findById(item.getSanPhamId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm mã " + item.getSanPhamId() + " không tồn tại!"));

            // [ĐÃ SỬA]: Chống Race Condition bằng Atomic Update trực tiếp dưới DB
            int updatedRows = sanPhamRepository.truSoLuongTon(item.getSanPhamId(), item.getSoLuong());

            if (updatedRows == 0) {
                throw new RuntimeException("Sản phẩm [" + sanPham.getTenSanPham() + "] không đủ số lượng trong kho lúc này!");
            }

            // Tính tiền món
            BigDecimal itemTotal = sanPham.getDonGia().multiply(BigDecimal.valueOf(item.getSoLuong()));
            totalAmount = totalAmount.add(itemTotal);

            // Tạo chi tiết đơn hàng
            ChiTietDonHang chiTiet = new ChiTietDonHang();
            chiTiet.setDonHang(savedDonHang);
            chiTiet.setSanPham(sanPham);
            chiTiet.setSoLuong(item.getSoLuong());
            chiTiet.setDonGiaTaiThoiDiem(sanPham.getDonGia());
            chiTietList.add(chiTiet);

            // Format dòng in hóa đơn: Tên bánh x Số lượng -> Thành tiền
            receiptItems.append(String.format("%-18s x%d   %s\n",
                    sanPham.getTenSanPham(), item.getSoLuong(), itemTotal.toString()));
        }

        chiTietDonHangRepository.saveAll(chiTietList);
        savedDonHang.setTongTien(totalAmount);
        donHangRepository.save(savedDonHang);

        // 5. TÍCH HỢP VIETQR (Bắn link ảnh QR động thông qua dịch vụ miễn phí của VietQR.io)
        String bankId = "vcb";
        String accountNo = "1234567890";
        String template = "qr_only";
        String addInfo = "PAY_POS_HD" + savedDonHang.getId();

        String vietQrUrl = String.format("https://img.vietqr.io/image/%s-%s-%s.png?amount=%s&addInfo=%s",
                bankId, accountNo, template, totalAmount.toBigInteger().toString(), addInfo);

        // 6. BUILD BIÊN LAI IN NHIỆT (Receipt) cho máy in tại quầy
        String receiptText = buildReceiptTemplate(savedDonHang, receiptItems.toString(), nhanVien.getHoTen());

        // 7. Map dữ liệu trả về cho Response DTO
        PosOrderDto.Response response = new PosOrderDto.Response();
        response.setDonHangId(savedDonHang.getId());
        response.setTongTien(totalAmount);
        response.setTrangThai(savedDonHang.getTrangThai());
        response.setNguonDon(savedDonHang.getNguonDon());
        response.setVietQrUrl(vietQrUrl);
        response.setReceiptText(receiptText);

        return response;
    }

    // Hàm phụ định dạng văn bản in hóa đơn cân đối
    private String buildReceiptTemplate(DonHang donHang, String itemsText, String tenNhanVien) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return "======= TIỆM BÁNH POLY CAKE =======" + "\n" +
                "Đ/C: Toà nhà FPT Polytechnic, HCM" + "\n" +
                "HÓA ĐƠN BÁN HÀNG TẠI QUẦY" + "\n" +
                "-----------------------------------" + "\n" +
                "Mã HD: HD-POS-" + donHang.getId() + "\n" +
                "Ngày: " + LocalDateTime.now().format(formatter) + "\n" +
                "Thu ngân: " + tenNhanVien + "\n" +
                "-----------------------------------" + "\n" +
                itemsText +
                "-----------------------------------" + "\n" +
                "TỔNG TIỀN: " + donHang.getTongTien().toString() + " VND\n" +
                "===================================" + "\n" +
                "  CẢM ƠN QUÝ KHÁCH - HẸN GẶP LẠI!  " + "\n";
    }
}