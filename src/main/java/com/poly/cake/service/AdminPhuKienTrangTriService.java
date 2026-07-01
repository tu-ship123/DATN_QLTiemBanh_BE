package com.poly.cake.service;

import com.poly.cake.entity.PhuKienTrangTri;
import com.poly.cake.repository.PhuKienTrangTriRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class AdminPhuKienTrangTriService {

    @Autowired
    private PhuKienTrangTriRepository repo;

    // ── DTO nội bộ ──────────────────────────────────────────────────────────────
    public static class PhuKienRequest {
        public String tenPhuKien;
        public BigDecimal donGia;
        public Integer soLuongTon;
        public String anhPhuKien;
        public Boolean hoatDong;
    }

    public static class PhuKienResponse {
        public Long id;
        public String tenPhuKien;
        public BigDecimal donGia;
        public Integer soLuongTon;
        public String anhPhuKien;
        public Boolean hoatDong;
        public String ngayTao;

        public PhuKienResponse(PhuKienTrangTri e) {
            this.id          = e.getId();
            this.tenPhuKien  = e.getTenPhuKien();
            this.donGia      = e.getDonGia();
            this.soLuongTon  = e.getSoLuongTon();
            this.anhPhuKien  = e.getAnhPhuKien();
            this.hoatDong    = e.getHoatDong();
            this.ngayTao     = e.getNgayTao() != null ? e.getNgayTao().toString() : null;
        }
    }

    // ── 1. DANH SÁCH ────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<PhuKienResponse> getAll(String keyword, Boolean hoatDong) {
        return repo.findAll().stream()
                .filter(e -> keyword == null || keyword.isBlank()
                        || e.getTenPhuKien().toLowerCase().contains(keyword.toLowerCase().trim()))
                .filter(e -> hoatDong == null || e.getHoatDong().equals(hoatDong))
                .map(PhuKienResponse::new)
                .toList();
    }

    // ── 2. CHI TIẾT ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PhuKienResponse getById(Long id) {
        return new PhuKienResponse(findOrThrow(id));
    }

    // ── 3. THÊM ─────────────────────────────────────────────────────────────────
    @Transactional
    public PhuKienResponse create(PhuKienRequest req) {
        PhuKienTrangTri e = new PhuKienTrangTri();
        e.setTenPhuKien(req.tenPhuKien);
        e.setDonGia(req.donGia);
        e.setSoLuongTon(req.soLuongTon != null ? req.soLuongTon : 0);
        e.setAnhPhuKien(req.anhPhuKien);
        e.setHoatDong(req.hoatDong != null ? req.hoatDong : true);
        return new PhuKienResponse(repo.save(e));
    }

    // ── 4. CẬP NHẬT ─────────────────────────────────────────────────────────────
    @Transactional
    public PhuKienResponse update(Long id, PhuKienRequest req) {
        PhuKienTrangTri e = findOrThrow(id);
        if (req.tenPhuKien  != null) e.setTenPhuKien(req.tenPhuKien);
        if (req.donGia      != null) e.setDonGia(req.donGia);
        if (req.soLuongTon  != null) e.setSoLuongTon(req.soLuongTon);
        if (req.anhPhuKien  != null) e.setAnhPhuKien(req.anhPhuKien);
        if (req.hoatDong    != null) e.setHoatDong(req.hoatDong);
        return new PhuKienResponse(repo.save(e));
    }

    // ── 5. XÓA ──────────────────────────────────────────────────────────────────
    @Transactional
    public void delete(Long id) {
        repo.delete(findOrThrow(id));
    }

    // ── 6. BẬT / TẮT HOẠT ĐỘNG ─────────────────────────────────────────────────
    @Transactional
    public PhuKienResponse toggle(Long id) {
        PhuKienTrangTri e = findOrThrow(id);
        e.setHoatDong(!Boolean.TRUE.equals(e.getHoatDong()));
        return new PhuKienResponse(repo.save(e));
    }

    // ── Helper ──────────────────────────────────────────────────────────────────
    private PhuKienTrangTri findOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phụ kiện với id = " + id));
    }
}
