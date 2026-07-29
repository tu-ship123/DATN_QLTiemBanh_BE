package com.poly.cake.service;

import com.poly.cake.dto.NgayLeLuongDto;
import com.poly.cake.entity.NgayLeLuong;
import com.poly.cake.exception.NgoaiLeNghiepVu;
import com.poly.cake.exception.NgoaiLeKhongTimThayTaiNguyen;
import com.poly.cake.repository.NgayLeLuongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * T102 – Quản lý cấu hình ngày lễ lương (Admin CRUD) và cung cấp hệ số
 * lương áp dụng cho 1 ngày cụ thể để ChamCongService dùng khi chấm công.
 */
@Service
@RequiredArgsConstructor
public class NgayLeLuongService {

    private final NgayLeLuongRepository ngayLeLuongRepository;

    public List<NgayLeLuongDto.Response> getAll() {
        return ngayLeLuongRepository.findAllByOrderByNgayLeAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public NgayLeLuongDto.Response getById(Long id) {
        return mapToResponse(timTheoId(id));
    }

    @Transactional
    public NgayLeLuongDto.Response create(NgayLeLuongDto.Request request) {
        if (ngayLeLuongRepository.existsByNgayLe(request.getNgayLe())) {
            throw new NgoaiLeNghiepVu("Ngày " + request.getNgayLe() + " đã được cấu hình là ngày lễ rồi");
        }

        NgayLeLuong entity = NgayLeLuong.builder()
                .ngayLe(request.getNgayLe())
                .tenNgayLe(request.getTenNgayLe())
                .heSoLuong(request.getHeSoLuong())
                .hoatDong(request.getHoatDong() == null ? true : request.getHoatDong())
                .build();

        return mapToResponse(ngayLeLuongRepository.save(entity));
    }

    @Transactional
    public NgayLeLuongDto.Response update(Long id, NgayLeLuongDto.Request request) {
        NgayLeLuong entity = timTheoId(id);

        // Nếu đổi sang 1 ngày khác thì phải đảm bảo ngày mới chưa bị trùng
        if (!entity.getNgayLe().equals(request.getNgayLe())
                && ngayLeLuongRepository.existsByNgayLe(request.getNgayLe())) {
            throw new NgoaiLeNghiepVu("Ngày " + request.getNgayLe() + " đã được cấu hình là ngày lễ rồi");
        }

        entity.setNgayLe(request.getNgayLe());
        entity.setTenNgayLe(request.getTenNgayLe());
        entity.setHeSoLuong(request.getHeSoLuong());
        entity.setHoatDong(request.getHoatDong() == null ? true : request.getHoatDong());

        return mapToResponse(ngayLeLuongRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        ngayLeLuongRepository.delete(timTheoId(id));
    }

    /**
     * Lấy hệ số lương áp dụng cho 1 ngày cụ thể.
     * Trả về 1.0 (bình thường) nếu ngày đó KHÔNG phải ngày lễ được cấu hình,
     * hoặc cấu hình của ngày đó đang bị tắt (hoatDong = false).
     */
    public BigDecimal layHeSoLuong(LocalDate ngay) {
        if (ngay == null) return BigDecimal.ONE;
        return ngayLeLuongRepository.findByNgayLeAndHoatDongTrue(ngay)
                .map(NgayLeLuong::getHeSoLuong)
                .orElse(BigDecimal.ONE);
    }

    private NgayLeLuong timTheoId(Long id) {
        return ngayLeLuongRepository.findById(id)
                .orElseThrow(() -> new NgoaiLeKhongTimThayTaiNguyen("Không tìm thấy cấu hình ngày lễ với id: " + id));
    }

    private NgayLeLuongDto.Response mapToResponse(NgayLeLuong entity) {
        NgayLeLuongDto.Response dto = new NgayLeLuongDto.Response();
        dto.setId(entity.getId());
        dto.setNgayLe(entity.getNgayLe());
        dto.setTenNgayLe(entity.getTenNgayLe());
        dto.setHeSoLuong(entity.getHeSoLuong());
        dto.setHoatDong(entity.getHoatDong());
        return dto;
    }
}