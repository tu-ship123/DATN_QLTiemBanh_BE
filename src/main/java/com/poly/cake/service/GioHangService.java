package com.poly.cake.service;

import com.poly.cake.exception.BusinessException;
import com.poly.cake.exception.ResourceNotFoundException;

import com.poly.cake.dto.GioHangDto;
import com.poly.cake.entity.ChiTietGioHang;
import com.poly.cake.entity.GioHang;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.SanPham;
import com.poly.cake.repository.GioHangRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.SanPhamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GioHangService {

    private final GioHangRepository gioHangRepository;

    private final NguoiDungRepository nguoiDungRepository;

    private final SanPhamRepository sanPhamRepository;

    // ─── LẤY GIỎ HÀNG CỦA USER (tạo mới nếu chưa có) ──────────────────────
    @Transactional
    public GioHangDto.GioHangResponse layGioHang(String email) {
        NguoiDung nguoiDung = timNguoiDung(email);
        GioHang gioHang = layHoacTaoGioHang(nguoiDung);
        return mapToGioHangResponse(gioHang);
    }

    // ─── THÊM SẢN PHẨM VÀO GIỎ ─────────────────────────────────────────────
    @Transactional
    public GioHangDto.GioHangResponse themVaoGio(String email, GioHangDto.ThemVaoGioRequest request) {
        NguoiDung nguoiDung = timNguoiDung(email);
        GioHang gioHang = layHoacTaoGioHang(nguoiDung);

        SanPham sanPham = sanPhamRepository.findById(request.getSanPhamId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + request.getSanPhamId()));

        if (!"DANG_BAN".equals(sanPham.getTrangThai())) {
            throw new BusinessException("Sản phẩm \"" + sanPham.getTenSanPham() + "\" hiện không còn bán.");
        }

        if (sanPham.getSoLuongTon() <= 0) {
            throw new BusinessException("Sản phẩm \"" + sanPham.getTenSanPham() + "\" đã hết hàng.");
        }

        // Kiểm tra xem sản phẩm đã có trong giỏ chưa (không tính thiết kế 3D)
        Optional<ChiTietGioHang> chiTietTonTai = gioHang.getChiTietGioHangs().stream()
                .filter(ct -> ct.getSanPham().getId().equals(sanPham.getId())
                        && Objects.equals(ct.getThietKeBanhJson(), request.getThietKeBanhJson()))
                .findFirst();

        if (chiTietTonTai.isPresent()) {
            // Tăng số lượng nếu đã có
            ChiTietGioHang chiTiet = chiTietTonTai.get();
            int soLuongMoi = chiTiet.getSoLuong() + request.getSoLuong();
            if (soLuongMoi > sanPham.getSoLuongTon()) {
                throw new BusinessException("Số lượng vượt quá tồn kho! Còn lại: " + sanPham.getSoLuongTon());
            }
            chiTiet.setSoLuong(soLuongMoi);
        } else {
            // Thêm mới
            if (request.getSoLuong() > sanPham.getSoLuongTon()) {
                throw new BusinessException("Số lượng vượt quá tồn kho! Còn lại: " + sanPham.getSoLuongTon());
            }
            ChiTietGioHang chiTietMoi = ChiTietGioHang.builder()
                    .gioHang(gioHang)
                    .sanPham(sanPham)
                    .soLuong(request.getSoLuong())
                    .thietKeBanhJson(request.getThietKeBanhJson())
                    .build();
            gioHang.getChiTietGioHangs().add(chiTietMoi);
        }

        gioHangRepository.save(gioHang);
        return mapToGioHangResponse(gioHang);
    }

    // ─── CẬP NHẬT SỐ LƯỢNG SẢN PHẨM TRONG GIỎ ─────────────────────────────
    @Transactional
    public GioHangDto.GioHangResponse capNhatSoLuong(String email, Long chiTietId, GioHangDto.CapNhatSoLuongRequest request) {
        NguoiDung nguoiDung = timNguoiDung(email);
        GioHang gioHang = layHoacTaoGioHang(nguoiDung);

        ChiTietGioHang chiTiet = gioHang.getChiTietGioHangs().stream()
                .filter(ct -> ct.getId().equals(chiTietId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng!"));

        if (request.getSoLuong() <= 0) {
            // Xóa nếu số lượng <= 0
            gioHang.getChiTietGioHangs().remove(chiTiet);
        } else {
            if (request.getSoLuong() > chiTiet.getSanPham().getSoLuongTon()) {
                throw new BusinessException("Số lượng vượt quá tồn kho! Còn lại: " + chiTiet.getSanPham().getSoLuongTon());
            }
            chiTiet.setSoLuong(request.getSoLuong());
        }

        gioHangRepository.save(gioHang);
        return mapToGioHangResponse(gioHang);
    }

    // ─── XÓA 1 SẢN PHẨM KHỎI GIỎ ───────────────────────────────────────────
    @Transactional
    public GioHangDto.GioHangResponse xoaKhoiGio(String email, Long chiTietId) {
        NguoiDung nguoiDung = timNguoiDung(email);
        GioHang gioHang = layHoacTaoGioHang(nguoiDung);

        boolean removed = gioHang.getChiTietGioHangs()
                .removeIf(ct -> ct.getId().equals(chiTietId));

        if (!removed) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng!");
        }

        gioHangRepository.save(gioHang);
        return mapToGioHangResponse(gioHang);
    }

    // ─── XÓA TOÀN BỘ GIỎ HÀNG ──────────────────────────────────────────────
    @Transactional
    public void xoaToanBoGio(String email) {
        NguoiDung nguoiDung = timNguoiDung(email);
        GioHang gioHang = layHoacTaoGioHang(nguoiDung);
        gioHang.getChiTietGioHangs().clear();
        gioHangRepository.save(gioHang);
    }

    // ─── HELPER: Tìm người dùng ──────────────────────────────────────────────
    private NguoiDung timNguoiDung(String email) {
        return nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + email));
    }

    // ─── HELPER: Lấy hoặc tạo giỏ hàng ─────────────────────────────────────
    private GioHang layHoacTaoGioHang(NguoiDung nguoiDung) {
        return gioHangRepository.findByKhachHang(nguoiDung)
                .orElseGet(() -> {
                    GioHang moi = GioHang.builder()
                            .khachHang(nguoiDung)
                            .build();
                    return gioHangRepository.save(moi);
                });
    }

    // ─── HELPER: Chuyển đổi Entity → DTO ────────────────────────────────────
    private GioHangDto.GioHangResponse mapToGioHangResponse(GioHang gioHang) {
        List<GioHangDto.ChiTietGioHangResponse> items = gioHang.getChiTietGioHangs().stream()
                .map(this::mapToChiTietResponse)
                .collect(Collectors.toList());

        BigDecimal tongTienHang = items.stream()
                .map(GioHangDto.ChiTietGioHangResponse::getThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int tongSoLuong = items.stream()
                .mapToInt(GioHangDto.ChiTietGioHangResponse::getSoLuong)
                .sum();

        // Miễn phí ship cho đơn >= 500,000đ
        BigDecimal phiShip = tongTienHang.compareTo(BigDecimal.valueOf(500000)) >= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(30000);

        BigDecimal tongThanhToan = tongTienHang.add(phiShip);

        GioHangDto.GioHangResponse response = new GioHangDto.GioHangResponse();
        response.setId(gioHang.getId());
        response.setItems(items);
        response.setTongSoLuong(tongSoLuong);
        response.setTongTienHang(tongTienHang);
        response.setPhiShip(phiShip);
        response.setTongThanhToan(tongThanhToan);
        response.setNgayCapNhat(gioHang.getNgayCapNhat());
        return response;
    }

    private GioHangDto.ChiTietGioHangResponse mapToChiTietResponse(ChiTietGioHang ct) {
        GioHangDto.ChiTietGioHangResponse item = new GioHangDto.ChiTietGioHangResponse();
        item.setId(ct.getId());
        item.setSanPhamId(ct.getSanPham().getId());
        item.setTenSanPham(ct.getSanPham().getTenSanPham());
        item.setAnhSanPham(ct.getSanPham().getAnhSanPham());
        item.setTenDanhMuc(ct.getSanPham().getDanhMuc() != null
                ? ct.getSanPham().getDanhMuc().getTenDanhMuc() : null);
        item.setDonGia(ct.getSanPham().getDonGia());
        item.setSoLuong(ct.getSoLuong());
        item.setThanhTien(ct.getSanPham().getDonGia().multiply(BigDecimal.valueOf(ct.getSoLuong())));
        item.setThietKeBanhJson(ct.getThietKeBanhJson());
        item.setNgayTao(ct.getNgayTao());
        return item;
    }
}
