package com.poly.cake.service;

import com.poly.cake.dto.PhuThuDonHangDto;
import com.poly.cake.entity.PhuThuDonHang;
import com.poly.cake.exception.BusinessException;
import com.poly.cake.exception.ResourceNotFoundException;
import com.poly.cake.repository.PhuThuDonHangRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * T102 – Quản lý cấu hình phụ thu dịp đặc biệt (Admin CRUD) và tính %
 * phụ thu áp dụng cho 1 ngày cụ thể để OrderService/PosOrderService dùng
 * khi tạo đơn hàng.
 */
@Service
@RequiredArgsConstructor
public class PhuThuDonHangService {

    private final PhuThuDonHangRepository phuThuDonHangRepository;

    public List<PhuThuDonHangDto.Response> getAll() {
        return phuThuDonHangRepository.findAllByOrderByNgayBatDauDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PhuThuDonHangDto.Response getById(Long id) {
        return mapToResponse(timTheoId(id));
    }

    @Transactional
    public PhuThuDonHangDto.Response create(PhuThuDonHangDto.Request request) {
        kiemTraNgayHopLe(request);
        PhuThuDonHang entity = PhuThuDonHang.builder()
                .tenDip(request.getTenDip())
                .ngayBatDau(request.getNgayBatDau())
                .ngayKetThuc(request.getNgayKetThuc())
                .phanTramPhuThu(request.getPhanTramPhuThu())
                .hoatDong(request.getHoatDong() == null ? true : request.getHoatDong())
                .build();
        return mapToResponse(phuThuDonHangRepository.save(entity));
    }

    @Transactional
    public PhuThuDonHangDto.Response update(Long id, PhuThuDonHangDto.Request request) {
        kiemTraNgayHopLe(request);
        PhuThuDonHang entity = timTheoId(id);
        entity.setTenDip(request.getTenDip());
        entity.setNgayBatDau(request.getNgayBatDau());
        entity.setNgayKetThuc(request.getNgayKetThuc());
        entity.setPhanTramPhuThu(request.getPhanTramPhuThu());
        entity.setHoatDong(request.getHoatDong() == null ? true : request.getHoatDong());
        return mapToResponse(phuThuDonHangRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        phuThuDonHangRepository.delete(timTheoId(id));
    }

    /**
     * Tính tổng % phụ thu áp dụng cho 1 ngày cụ thể (thường là ngày giao hàng
     * của đơn). Nếu nhiều dịp đặc biệt đang hoạt động trùng ngày, CỘNG DỒN %
     * của tất cả các dịp lại. Trả về 0 nếu ngày đó không rơi vào dịp nào.
     */
    public BigDecimal tinhPhanTramPhuThu(LocalDate ngay) {
        if (ngay == null) return BigDecimal.ZERO;
        return phuThuDonHangRepository.findApDungTheoNgay(ngay)
                .stream()
                .map(PhuThuDonHang::getPhanTramPhuThu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void kiemTraNgayHopLe(PhuThuDonHangDto.Request request) {
        if (request.getNgayKetThuc().isBefore(request.getNgayBatDau())) {
            throw new BusinessException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
    }

    private PhuThuDonHang timTheoId(Long id) {
        return phuThuDonHangRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình phụ thu với id: " + id));
    }

    private PhuThuDonHangDto.Response mapToResponse(PhuThuDonHang entity) {
        PhuThuDonHangDto.Response dto = new PhuThuDonHangDto.Response();
        dto.setId(entity.getId());
        dto.setTenDip(entity.getTenDip());
        dto.setNgayBatDau(entity.getNgayBatDau());
        dto.setNgayKetThuc(entity.getNgayKetThuc());
        dto.setPhanTramPhuThu(entity.getPhanTramPhuThu());
        dto.setHoatDong(entity.getHoatDong());
        return dto;
    }
}