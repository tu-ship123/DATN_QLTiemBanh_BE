package com.poly.cake.service;

import com.poly.cake.exception.BusinessException;
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

    // Ngưỡng cảnh báo tồn kho mặc định cố định = 10 khi admin không truyền riêng
    private static final int NGUONG_CANH_BAO_MAC_DINH = 10;

    private final SanPhamRepository sanPhamRepository;

    private final DanhMucRepository danhMucRepository;

    private final InventoryService inventoryService;

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
     *
     * LƯU Ý: sản phẩm này bị loại khỏi mọi danh sách sản phẩm (xem
     * SanPhamRepository.filterProducts) nên sẽ KHÔNG hiện ra ở trang khách xem hàng
     * hay trang quản lý admin - nó chỉ tồn tại trong DB để ChiTietGioHang/ChiTietDonHang
     * có 1 sanPhamId hợp lệ để trỏ vào, giá thật luôn lấy từ donGiaTuyChinh.
     */
    @Transactional
    public SanPhamDto.Response getOrCreateCustomCakeMarker() {
        // "Tự chữa lành" dữ liệu cũ: gom TẤT CẢ bản ghi có thể là marker (đã có cờ
        // laNoiBo=true, HOẶC tên khớp dù lệch khoảng trắng/hoa-thường do lỗi cũ từng
        // tạo trùng nhiều bản) - luôn dùng bản ghi CŨ NHẤT làm chuẩn, các bản dư thừa
        // sẽ được đánh dấu laNoiBo=true để không bao giờ lọt ra ngoài danh sách nữa,
        // dù tên của chúng có đúng tuyệt đối 100% với TEN_SAN_PHAM_CUSTOM_CAKE hay không.
        List<SanPham> ungVien = sanPhamRepository.timCacBanGhiCoTheLaCustomCakeMarker(TEN_SAN_PHAM_CUSTOM_CAKE);

        SanPham sanPham;
        if (!ungVien.isEmpty()) {
            sanPham = ungVien.get(0); // cũ nhất
            boolean coThayDoi = false;
            for (SanPham banGhi : ungVien) {
                if (!Boolean.TRUE.equals(banGhi.getLaNoiBo())) {
                    banGhi.setLaNoiBo(true);
                    coThayDoi = true;
                }
            }
            if (coThayDoi) {
                sanPhamRepository.saveAll(ungVien);
            }
        } else {
            sanPham = taoMoiCustomCakeMarker();
        }
        return mapToResponseDto(sanPham);
    }

    private SanPham taoMoiCustomCakeMarker() {
        SanPham moi = new SanPham();
        moi.setTenSanPham(TEN_SAN_PHAM_CUSTOM_CAKE);
        // Giá hiển thị mặc định (chỉ mang tính tham khảo) - giá thật luôn
        // được ghi đè bởi donGiaTuyChinh của từng chi tiết giỏ hàng.
        moi.setDonGia(java.math.BigDecimal.valueOf(420000));
        // Số lượng lớn tượng trưng cho "làm theo yêu cầu, không giới hạn tồn kho"
        moi.setSoLuongTon(999999);
        moi.setTrangThai("DANG_BAN");
        moi.setLaNoiBo(true); // <-- cờ QUYẾT ĐỊNH việc ẩn khỏi mọi danh sách sản phẩm
        moi.setMoTa("Sản phẩm đại diện dùng chung cho mọi chiếc bánh khách tự thiết kế "
                + "ở công cụ 3D. Giá thật của từng đơn được tính riêng theo lựa chọn của khách.");
        return sanPhamRepository.save(moi);
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

    // LẤY DANH SÁCH SẢN PHẨM TỒN KHO THẤP CẦN CẢNH BÁO
    @Transactional(readOnly = true)
    public List<SanPhamDto.Response> getLowStockProducts() {
        return sanPhamRepository.findLowStockProducts()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
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
        sanPham.setNguongCanhBao(
                request.getNguongCanhBao() != null
                        ? request.getNguongCanhBao()
                        : NGUONG_CANH_BAO_MAC_DINH
        );

        if (request.getTrangThai() != null
                && !request.getTrangThai().isBlank()) {
            sanPham.setTrangThai(request.getTrangThai());
        }

        SanPham saved = sanPhamRepository.save(sanPham);

        // Sản phẩm mới thêm có thể được nhập với số lượng tồn ban đầu đã thấp hơn
        // ngưỡng cảnh báo -> kiểm tra và gửi cảnh báo ngay nếu cần.
        inventoryService.kiemTraVaCanhBaoNeuTonKhoThap(saved);

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
        sanPham.setNguongCanhBao(
                request.getNguongCanhBao() != null
                        ? request.getNguongCanhBao()
                        : NGUONG_CANH_BAO_MAC_DINH
        );

        SanPham updated = sanPhamRepository.save(sanPham);

        inventoryService.kiemTraVaCanhBaoNeuTonKhoThap(updated);

        return mapToResponseDto(updated);
    }

    // 8. CẬP NHẬT TỒN KHO (nhập thêm hàng / điều chỉnh tay từ trang admin)
    // soLuongThayDoi > 0: nhập thêm hàng | soLuongThayDoi < 0: xuất/điều chỉnh giảm
    // Sau khi cập nhật, tự động kiểm tra ngưỡng cảnh báo tồn kho thấp (mặc định = 10).
    @Transactional
    public SanPhamDto.Response capNhatTonKho(Long id, Integer soLuongThayDoi) {

        sanPhamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        if (soLuongThayDoi == null || soLuongThayDoi == 0) {
            throw new BusinessException("Số lượng thay đổi phải khác 0");
        }

        if (soLuongThayDoi > 0) {
            sanPhamRepository.congLaiSoLuongTon(id, soLuongThayDoi);
        } else {
            int soDongBiAnhHuong = sanPhamRepository.truSoLuongTon(id, -soLuongThayDoi);
            if (soDongBiAnhHuong == 0) {
                throw new BusinessException(
                        "Số lượng tồn kho hiện tại không đủ để trừ " + (-soLuongThayDoi));
            }
        }

        // Đọc lại bản ghi mới nhất sau UPDATE, rồi kiểm tra + gửi cảnh báo nếu cần
        SanPham sanPhamMoiNhat = sanPhamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        inventoryService.kiemTraVaCanhBaoNeuTonKhoThap(sanPhamMoiNhat);

        return mapToResponseDto(sanPhamMoiNhat);
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
        dto.setNguongCanhBao(sanPham.getNguongCanhBao());

        return dto;
    }
}