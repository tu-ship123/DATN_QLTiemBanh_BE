package com.poly.cake.service;

import com.poly.cake.exception.ResourceNotFoundException;

import com.poly.cake.dto.SanPhamDto;
import com.poly.cake.entity.DanhMuc;
import com.poly.cake.entity.SanPham;
import com.poly.cake.repository.DanhMucRepository;
import com.poly.cake.repository.SanPhamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSanPhamService {

    private final SanPhamRepository sanPhamRepository;

    private final DanhMucRepository danhMucRepository;

    /**
     * Tên sản phẩm "đại diện" dùng chung cho MỌI chiếc bánh khách tự thiết kế ở
     * CakeBuilder3D (xem Design.vue -> datBanhNay()). Bản thân sản phẩm này không có
     * giá cố định thật sự - giá thật của từng chiếc bánh được tính riêng ở FE theo
     * size/số tầng/phụ kiện và lưu vào ChiTietGioHang.donGiaTuyChinh khi thêm vào giỏ.
     */
    private static final String TEN_SAN_PHAM_CUSTOM_CAKE = "Bánh thiết kế 3D tùy chỉnh";

    /**
     * Lấy sản phẩm đại diện cho bánh 3D tùy chỉnh, tự động tạo nếu chưa tồn tại
     * (khởi tạo project lần đầu chưa có sẵn trong dữ liệu mẫu).
     * GET /api/v1/products/custom-cake-marker (KhachHangSanPhamController) gọi hàm này.
     */
    @Transactional
    public SanPhamDto.Response getOrCreateCustomCakeMarker() {
        SanPham sanPham = sanPhamRepository.findByTenSanPham(TEN_SAN_PHAM_CUSTOM_CAKE)
                .orElseGet(() -> {
                    SanPham moi = new SanPham();
                    moi.setTenSanPham(TEN_SAN_PHAM_CUSTOM_CAKE);
                    // Giá hiển thị mặc định (chỉ mang tính tham khảo) - giá thật luôn
                    // được ghi đè bởi donGiaTuyChinh của từng chi tiết giỏ hàng.
                    moi.setDonGia(java.math.BigDecimal.valueOf(420000));
                    // Số lượng lớn tượng trưng cho "làm theo yêu cầu, không giới hạn tồn kho"
                    moi.setSoLuongTon(999999);
                    moi.setTrangThai("DANG_BAN");
                    moi.setMoTa("Sản phẩm đại diện dùng chung cho mọi chiếc bánh khách tự thiết kế "
                            + "ở công cụ 3D. Giá thật của từng đơn được tính riêng theo lựa chọn của khách.");
                    return sanPhamRepository.save(moi);
                });
        return mapToResponseDto(sanPham);
    }

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
                        new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        return mapToResponseDto(sanPham);
    }

    // 3. THÊM SẢN PHẨM
    @Transactional
    public SanPhamDto.Response createProduct(
            SanPhamDto.Request request) {

        DanhMuc danhMuc = danhMucRepository.findById(
                        request.getDanhMucId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy danh mục"));

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
                        new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        DanhMuc danhMuc = danhMucRepository.findById(
                        request.getDanhMucId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy danh mục"));

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
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        sanPhamRepository.delete(sanPham);
    }

    // 6. ẨN SẢN PHẨM
    @Transactional
    public SanPhamDto.Response hideProduct(Long id) {

        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy sản phẩm"));

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
                        new ResourceNotFoundException("Không tìm thấy sản phẩm"));

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