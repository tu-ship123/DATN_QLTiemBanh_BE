package com.poly.cake.service;

import com.poly.cake.dto.SanPhamDto;
import com.poly.cake.entity.DanhMuc;
import com.poly.cake.entity.SanPham;
import com.poly.cake.repository.DanhMucRepository;
import com.poly.cake.repository.SanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminSanPhamService {

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    // 1. DANH SÁCH + LỌC + TÌM KIẾM
    @Transactional(readOnly = true)
    public List<SanPhamDto.Response> getFilteredProducts(
            String keyword,
            String trangThai,
            Long danhMucId) {

        return sanPhamRepository
                .filterProducts(keyword, trangThai, danhMucId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // 2. CHI TIẾT SẢN PHẨM
    @Transactional(readOnly = true)
    public SanPhamDto.Response getProductById(Long id) {

        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy sản phẩm"));

        return mapToResponseDto(sanPham);
    }

    // 3. THÊM SẢN PHẨM
    @Transactional
    public SanPhamDto.Response createProduct(
            SanPhamDto.Request request) {

        DanhMuc danhMuc = danhMucRepository.findById(
                        request.getDanhMucId())
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy danh mục"));

        SanPham sanPham = new SanPham();

        sanPham.setDanhMuc(danhMuc);
        sanPham.setTenSanPham(request.getTenSanPham());
        sanPham.setDonGia(request.getDonGia());
        sanPham.setSoLuongTon(request.getSoLuongTon());
        sanPham.setAnhSanPham(request.getAnhSanPham());
        sanPham.setMoTa(request.getMoTa());

        if (request.getTrangThai() != null
                && !request.getTrangThai().isBlank()) {
            sanPham.setTrangThai(request.getTrangThai());
        }

        SanPham saved = sanPhamRepository.save(sanPham);

        return mapToResponseDto(saved);
    }

    // 4. CẬP NHẬT SẢN PHẨM
    @Transactional
    public SanPhamDto.Response updateProduct(
            Long id,
            SanPhamDto.Request request) {

        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy sản phẩm"));

        DanhMuc danhMuc = danhMucRepository.findById(
                        request.getDanhMucId())
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy danh mục"));

        sanPham.setDanhMuc(danhMuc);
        sanPham.setTenSanPham(request.getTenSanPham());
        sanPham.setDonGia(request.getDonGia());
        sanPham.setSoLuongTon(request.getSoLuongTon());
        sanPham.setAnhSanPham(request.getAnhSanPham());
        sanPham.setMoTa(request.getMoTa());
        sanPham.setTrangThai(request.getTrangThai());

        SanPham updated = sanPhamRepository.save(sanPham);

        return mapToResponseDto(updated);
    }

    // 5. XÓA SẢN PHẨM
    @Transactional
    public void deleteProduct(Long id) {
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
        sanPham.setTrangThai("DA_XOA"); // Soft delete
        sanPhamRepository.save(sanPham);
    }

    // 6. ẨN SẢN PHẨM
    @Transactional
    public SanPhamDto.Response hideProduct(Long id) {

        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy sản phẩm"));

        sanPham.setTrangThai("TAM_AN");

        return mapToResponseDto(
                sanPhamRepository.save(sanPham)
        );
    }

    // 7. KÍCH HOẠT LẠI SẢN PHẨM
    @Transactional
    public SanPhamDto.Response activeProduct(Long id) {

        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy sản phẩm"));

        sanPham.setTrangThai("DANG_BAN");

        return mapToResponseDto(
                sanPhamRepository.save(sanPham)
        );
    }

    private SanPhamDto.Response mapToResponseDto(
            SanPham sanPham) {

        SanPhamDto.Response dto =
                new SanPhamDto.Response();

        dto.setId(sanPham.getId());

        if (sanPham.getDanhMuc() != null) {
            dto.setDanhMucId(sanPham.getDanhMuc().getId());
            dto.setTenDanhMuc(
                    sanPham.getDanhMuc().getTenDanhMuc()
            );
        }

        dto.setTenSanPham(sanPham.getTenSanPham());
        dto.setDonGia(sanPham.getDonGia());
        dto.setSoLuongTon(sanPham.getSoLuongTon());
        dto.setAnhSanPham(sanPham.getAnhSanPham());
        dto.setTrangThai(sanPham.getTrangThai());
        dto.setMoTa(sanPham.getMoTa());
        dto.setNgayTao(sanPham.getNgayTao());

        return dto;
    }
}