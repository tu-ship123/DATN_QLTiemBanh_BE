package com.poly.cake.service;

import com.poly.cake.dto.ThietKeYeuThichDto;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.ThietKeYeuThich;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.ThietKeYeuThichRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ThietKeYeuThichService {

    @Autowired
    private ThietKeYeuThichRepository yeuThichRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    public List<ThietKeYeuThich> getDanhSachYeuThich(String email) {
        Long userId = timNguoiDung(email).getId();
        return yeuThichRepository.findByKhachHangId(userId);
    }

    public ThietKeYeuThich luuThietKe(String email, ThietKeYeuThichDto request) {
        Long userId = timNguoiDung(email).getId();

        // Tránh trùng lặp cùng một thiết kế json
        if (yeuThichRepository.existsByKhachHangIdAndThietKeBanhJson(userId, request.getThietKeBanhJson())) {
            throw new RuntimeException("Thiết kế này đã nằm trong danh sách yêu thích!");
        }

        ThietKeYeuThich thietKe = new ThietKeYeuThich();
        thietKe.setKhachHangId(userId);
        thietKe.setTenThietKe(request.getTenThietKe());
        thietKe.setThietKeBanhJson(request.getThietKeBanhJson());
        thietKe.setGia(request.getGia());

        return yeuThichRepository.save(thietKe);
    }

    public void xoaThietKe(String email, Long id) {
        Long userId = timNguoiDung(email).getId();
        ThietKeYeuThich thietKe = yeuThichRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu thiết kế yêu thích!"));

        if (!thietKe.getKhachHangId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa thiết kế này!");
        }
        yeuThichRepository.delete(thietKe);
    }

    private NguoiDung timNguoiDung(String email) {
        return nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));
    }
}
