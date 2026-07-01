package com.poly.cake.service;

import com.poly.cake.dto.PhuKienTrangTriDto;
import com.poly.cake.entity.PhuKienTrangTri;
import com.poly.cake.repository.PhuKienTrangTriRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * T050 - DecorPanel: Danh sách phụ kiện trang trí còn hàng cho khách hàng chọn.
 */
@Service
public class PhuKienTrangTriService {

    @Autowired
    private PhuKienTrangTriRepository phuKienTrangTriRepository;

    @Transactional(readOnly = true)
    public List<PhuKienTrangTriDto.Response> getAvailableAccessories() {
        return phuKienTrangTriRepository.findAll()
                .stream()
                // Chỉ hiển thị phụ kiện đang hoạt động và còn hàng
                .filter(pk -> Boolean.TRUE.equals(pk.getHoatDong()) && pk.getSoLuongTon() != null && pk.getSoLuongTon() > 0)
                .sorted(Comparator.comparing(PhuKienTrangTri::getTenPhuKien, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private PhuKienTrangTriDto.Response mapToResponseDto(PhuKienTrangTri entity) {
        PhuKienTrangTriDto.Response dto = new PhuKienTrangTriDto.Response();
        dto.setId(entity.getId());
        dto.setTenPhuKien(entity.getTenPhuKien());
        dto.setDonGia(entity.getDonGia());
        dto.setSoLuongTon(entity.getSoLuongTon());
        dto.setAnhPhuKien(entity.getAnhPhuKien());
        return dto;
    }
}
