package com.poly.cake.service;

import com.poly.cake.exception.BusinessException;
import com.poly.cake.exception.ResourceNotFoundException;
import com.poly.cake.exception.ForbiddenException;

import com.poly.cake.dto.DanhGiaDto;
import com.poly.cake.entity.DanhGia;
import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.SanPham;
import com.poly.cake.repository.DanhGiaRepository;
import com.poly.cake.repository.DonHangRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.SanPhamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DanhGiaService {

    private final DanhGiaRepository danhGiaRepository;
    private final DonHangRepository donHangRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final SanPhamRepository sanPhamRepository;

    // ─── Gửi đánh giá ────────────────────────────────────────────────────────

    @Transactional
    public DanhGiaDto.Response guiDanhGia(Long donHangId, DanhGiaDto.Request request, String emailKhach) {

        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailKhach)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        DonHang donHang = donHangRepository.findById(donHangId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + donHangId));

        // Chỉ chủ đơn mới được đánh giá
        if (!donHang.getKhachHang().getId().equals(khachHang.getId())) {
            throw new ForbiddenException("Bạn không có quyền đánh giá đơn hàng này");
        }

        // Chỉ đơn HOAN_THANH mới được đánh giá
        if (!"HOAN_THANH".equals(donHang.getTrangThai())) {
            throw new BusinessException("Chỉ có thể đánh giá đơn hàng đã hoàn thành");
        }

        SanPham sanPham = sanPhamRepository.findById(request.getSanPhamId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        // Kiểm tra sản phẩm có trong đơn không
        boolean sanPhamTrongDon = donHang.getChiTietDonHangs().stream()
                .anyMatch(ct -> ct.getSanPham().getId().equals(sanPham.getId()));
        if (!sanPhamTrongDon) {
            throw new BusinessException("Sản phẩm không thuộc đơn hàng này");
        }

        // Kiểm tra đã đánh giá sản phẩm này trong đơn chưa
        if (danhGiaRepository.existsByKhachHangAndDonHangAndSanPham(khachHang, donHang, sanPham)) {
            throw new BusinessException("Bạn đã đánh giá sản phẩm này trong đơn hàng này rồi");
        }

        DanhGia danhGia = DanhGia.builder()
                .khachHang(khachHang)
                .donHang(donHang)
                .sanPham(sanPham)
                .soSao(request.getSoSao())
                .noiDung(request.getNoiDung())
                .biAn(false)
                .build();

        danhGia = danhGiaRepository.save(danhGia);
        return toResponse(danhGia);
    }

    // ─── Lấy đánh giá của 1 đơn hàng ────────────────────────────────────────

    public DanhGiaDto.DonHangDanhGiaResponse layDanhGiaTheoDon(Long donHangId, String emailKhach) {

        NguoiDung khachHang = nguoiDungRepository.findByEmail(emailKhach)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        DonHang donHang = donHangRepository.findById(donHangId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        if (!donHang.getKhachHang().getId().equals(khachHang.getId())) {
            throw new ForbiddenException("Không có quyền xem đánh giá này");
        }

        List<DanhGia> danhSach = danhGiaRepository.findByDonHang(donHang);
        boolean daDanhGia = danhGiaRepository.existsByKhachHangAndDonHang(khachHang, donHang);

        DanhGiaDto.DonHangDanhGiaResponse res = new DanhGiaDto.DonHangDanhGiaResponse();
        res.setDonHangId(donHangId);
        res.setDaDanhGia(daDanhGia);
        res.setDanhSach(danhSach.stream().map(this::toResponse).collect(Collectors.toList()));
        return res;
    }

    // ─── Lấy đánh giá công khai của 1 sản phẩm ──────────────────────────────

    public List<DanhGiaDto.Response> layDanhGiaTheoSanPham(Long sanPhamId) {
        SanPham sanPham = sanPhamRepository.findById(sanPhamId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        return danhGiaRepository.findBySanPhamAndBiAnFalseOrderByNgayTaoDesc(sanPham)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── Helper map entity → DTO ──────────────────────────────────────────────

    private DanhGiaDto.Response toResponse(DanhGia dg) {
        DanhGiaDto.Response dto = new DanhGiaDto.Response();
        dto.setId(dg.getId());
        dto.setDonHangId(dg.getDonHang().getId());
        dto.setSanPhamId(dg.getSanPham().getId());
        dto.setTenSanPham(dg.getSanPham().getTenSanPham());
        dto.setAnhSanPham(dg.getSanPham().getAnhSanPham());
        dto.setTenKhachHang(dg.getKhachHang().getHoTen());
        dto.setSoSao(dg.getSoSao());
        dto.setNoiDung(dg.getNoiDung());
        dto.setPhanHoiCuaTiem(dg.getPhanHoiCuaTiem());
        dto.setBiAn(dg.getBiAn());
        dto.setNgayTao(dg.getNgayTao());
        return dto;
    }
}