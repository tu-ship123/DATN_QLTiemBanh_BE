package com.poly.cake.service;


import com.poly.cake.dto.PhieuNhapDto;
import com.poly.cake.entity.ChiTietPhieuNhap;
import com.poly.cake.entity.PhieuNhapKho;
import com.poly.cake.entity.SanPham;
import com.poly.cake.repository.PhieuNhapKhoRepository;
import com.poly.cake.repository.SanPhamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PhieuNhapService {

    private final PhieuNhapKhoRepository phieuNhapKhoRepository;
    private final SanPhamRepository sanPhamRepository;

    @Transactional
    public PhieuNhapKho taoPhieuNhap(PhieuNhapDto request, Long nguoiTaoId) {
        PhieuNhapKho phieu = new PhieuNhapKho();
        phieu.setNguoiTaoId(nguoiTaoId);
        phieu.setGhiChu(request.getGhiChu());
        phieu.setTrangThai("CHO_DUYET");

        double tongTien = 0;
        List<ChiTietPhieuNhap> chiTiets = new ArrayList<>();

        for (PhieuNhapDto.ChiTietNhapDto ctDto : request.getChiTietList()) {
            ChiTietPhieuNhap ct = new ChiTietPhieuNhap();
            ct.setPhieuNhapKho(phieu);
            ct.setSanPhamId(ctDto.getSanPhamId());
            ct.setSoLuong(ctDto.getSoLuong());
            ct.setGiaNhap(ctDto.getGiaNhap());
            
            tongTien += (ctDto.getSoLuong() * ctDto.getGiaNhap());
            chiTiets.add(ct);
        }

        phieu.setTongTien(tongTien);
        phieu.setChiTietList(chiTiets);
        return phieuNhapKhoRepository.save(phieu);
    }

    @Transactional
    public PhieuNhapKho duyetPhieuNhap(Long phieuNhapId, Long adminId) {
        PhieuNhapKho phieu = phieuNhapKhoRepository.findById(phieuNhapId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập"));

        if (!"CHO_DUYET".equals(phieu.getTrangThai())) {
            throw new RuntimeException("Phiếu này đã được duyệt hoặc hủy trước đó!");
        }

        // Đổi trạng thái
        phieu.setTrangThai("DA_DUYET");
        phieu.setNguoiDuyetId(adminId);
        phieu.setNgayDuyet(LocalDateTime.now());

        // Cộng dồn tồn kho cho từng sản phẩm
        for (ChiTietPhieuNhap ct : phieu.getChiTietList()) {
            SanPham sanPham = sanPhamRepository.findById(ct.getSanPhamId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: " + ct.getSanPhamId()));
            
            // Cộng thêm số lượng nhập vào số lượng tồn hiện tại
            sanPham.setSoLuongTon(sanPham.getSoLuongTon() + ct.getSoLuong());
            sanPhamRepository.save(sanPham);
        }

        return phieuNhapKhoRepository.save(phieu);
    }
}