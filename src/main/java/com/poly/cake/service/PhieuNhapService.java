package com.poly.cake.service;


import com.poly.cake.dto.PhieuNhapDto;
import com.poly.cake.dto.PhieuNhapResponseDto;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhieuNhapService {

    private final PhieuNhapKhoRepository phieuNhapKhoRepository;
    private final SanPhamRepository sanPhamRepository;

    // Danh sách toàn bộ phiếu nhập, mới nhất lên đầu - dùng cho trang PurchaseOrder (Admin)
    @Transactional(readOnly = true)
    public List<PhieuNhapResponseDto> layDanhSach() {
        List<PhieuNhapKho> phieus = phieuNhapKhoRepository.findAllByOrderByNgayTaoDesc();

        // Gom tất cả sanPhamId cần tra tên, tránh N+1
        List<Long> sanPhamIds = phieus.stream()
                .flatMap(p -> p.getChiTietList() == null ? java.util.stream.Stream.<ChiTietPhieuNhap>empty() : p.getChiTietList().stream())
                .map(ChiTietPhieuNhap::getSanPhamId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> tenSanPhamMap = new HashMap<>();
        if (!sanPhamIds.isEmpty()) {
            for (SanPham sp : sanPhamRepository.findAllById(sanPhamIds)) {
                tenSanPhamMap.put(sp.getId(), sp.getTenSanPham());
            }
        }

        return phieus.stream().map(p -> mapToDto(p, tenSanPhamMap)).collect(Collectors.toList());
    }

    private PhieuNhapResponseDto mapToDto(PhieuNhapKho p, Map<Long, String> tenSanPhamMap) {
        List<PhieuNhapResponseDto.ChiTiet> chiTiets = (p.getChiTietList() == null ? List.<ChiTietPhieuNhap>of() : p.getChiTietList())
                .stream()
                .map(ct -> new PhieuNhapResponseDto.ChiTiet(
                        ct.getSanPhamId(),
                        tenSanPhamMap.getOrDefault(ct.getSanPhamId(), "—"),
                        ct.getSoLuong(),
                        ct.getGiaNhap()
                ))
                .collect(Collectors.toList());

        return new PhieuNhapResponseDto(
                p.getId(), p.getNguoiTaoId(), p.getNguoiDuyetId(), p.getTrangThai(),
                p.getTongTien(), p.getGhiChu(), p.getNgayTao(), p.getNgayDuyet(), chiTiets
        );
    }

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