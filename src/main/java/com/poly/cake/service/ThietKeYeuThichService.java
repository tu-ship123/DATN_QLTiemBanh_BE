package com.poly.cake.service;

import com.poly.cake.dto.ThietKeYeuThichDto;
import com.poly.cake.entity.ThietKeYeuThich;
import com.poly.cake.repository.ThietKeYeuThichRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ThietKeYeuThichService {

    @Autowired
    private ThietKeYeuThichRepository yeuThichRepository;

    public List<ThietKeYeuThich> getDanhSachYeuThich(Long userId) {
        return yeuThichRepository.findByKhachHangId(userId);
    }

    public ThietKeYeuThich luuThietKe(Long userId, ThietKeYeuThichDto request) {
        // Tránh trùng lặp cùng một thiết kế json
        if (yeuThichRepository.existsByKhachHangIdAndThietKeBanhJson(userId, request.getThietKeBanhJson())) {
            throw new RuntimeException("Thiết kế này đã nằm trong danh sách yêu thích!");
        }

        ThietKeYeuThich thietKe = new ThietKeYeuThich();
        thietKe.setKhachHangId(userId);
        thietKe.setTenThietKe(request.getTenThietKe());
        thietKe.setThietKeBanhJson(request.getThietKeBanhJson());

        return yeuThichRepository.save(thietKe);
    }

    public void xoaThietKe(Long userId, Long id) {
        ThietKeYeuThich thietKe = yeuThichRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu thiết kế yêu thích!"));

        if (!thietKe.getKhachHangId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa thiết kế này!");
        }
        yeuThichRepository.delete(thietKe);
    }
}