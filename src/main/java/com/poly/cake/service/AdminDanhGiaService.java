package com.poly.cake.service;

import com.poly.cake.exception.ResourceNotFoundException;

import com.poly.cake.dto.DanhGiaDto;
import com.poly.cake.entity.DanhGia;
import com.poly.cake.entity.SanPham;
import com.poly.cake.repository.DanhGiaRepository;
import com.poly.cake.repository.SanPhamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service

@RequiredArgsConstructor
public class AdminDanhGiaService {

    private final DanhGiaRepository danhGiaRepository;
    private final SanPhamRepository sanPhamRepository;

    // ── Lấy tất cả + lọc ─────────────────────────────────────────────────────
    public List<DanhGiaDto.Response> getAll(Integer soSao, Long sanPhamId, Boolean biAn) {

        return danhGiaRepository.findAll().stream()
                .filter(dg -> soSao   == null || dg.getSoSao().equals(soSao))
                .filter(dg -> sanPhamId == null || dg.getSanPham().getId().equals(sanPhamId))
                .filter(dg -> biAn    == null || dg.getBiAn().equals(biAn))
                .sorted((a, b) -> b.getNgayTao().compareTo(a.getNgayTao()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Thống kê tổng quan ───────────────────────────────────────────────────
    public DanhGiaDto.StatsResponse getStats() {

        List<DanhGia> all = danhGiaRepository.findAll();

        long tong      = all.size();
        long chuaTraLoi = all.stream().filter(d -> d.getPhanHoiCuaTiem() == null || d.getPhanHoiCuaTiem().isBlank()).count();
        long biAn      = all.stream().filter(d -> Boolean.TRUE.equals(d.getBiAn())).count();

        double trungBinh = all.isEmpty() ? 0.0
                : all.stream().mapToInt(DanhGia::getSoSao).average().orElse(0.0);

        long nam5Sao = all.stream().filter(d -> d.getSoSao() == 5).count();
        long nam4Sao = all.stream().filter(d -> d.getSoSao() == 4).count();
        long nam3Sao = all.stream().filter(d -> d.getSoSao() == 3).count();
        long nam2Sao = all.stream().filter(d -> d.getSoSao() == 2).count();
        long nam1Sao = all.stream().filter(d -> d.getSoSao() == 1).count();

        DanhGiaDto.StatsResponse stats = new DanhGiaDto.StatsResponse();
        stats.setTong(tong);
        stats.setChuaTraLoi(chuaTraLoi);
        stats.setBiAn(biAn);
        stats.setTrungBinhSao(Math.round(trungBinh * 10.0) / 10.0);
        stats.setSao5(nam5Sao);
        stats.setSao4(nam4Sao);
        stats.setSao3(nam3Sao);
        stats.setSao2(nam2Sao);
        stats.setSao1(nam1Sao);
        return stats;
    }

    // ── Phản hồi đánh giá ────────────────────────────────────────────────────
    @Transactional
    public DanhGiaDto.Response reply(Long id, String phanHoi) {

        DanhGia dg = danhGiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá #" + id));

        dg.setPhanHoiCuaTiem(phanHoi);
        return toResponse(danhGiaRepository.save(dg));
    }

    // ── Ẩn / Hiện ────────────────────────────────────────────────────────────
    @Transactional
    public DanhGiaDto.Response toggleBiAn(Long id) {

        DanhGia dg = danhGiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá #" + id));

        dg.setBiAn(!Boolean.TRUE.equals(dg.getBiAn()));
        return toResponse(danhGiaRepository.save(dg));
    }

    // ── Xóa ──────────────────────────────────────────────────────────────────
    @Transactional
    public void delete(Long id) {

        DanhGia dg = danhGiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá #" + id));

        danhGiaRepository.delete(dg);
    }

    // ── Map entity → DTO ─────────────────────────────────────────────────────
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