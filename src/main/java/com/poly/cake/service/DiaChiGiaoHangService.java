package com.poly.cake.service;

import com.poly.cake.dto.DiaChiDto;
import com.poly.cake.entity.DiaChiGiaoHang;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.exception.MaxAddressLimitException;
import com.poly.cake.repository.DiaChiGiaoHangRepository;
import com.poly.cake.repository.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DiaChiGiaoHangService {

    @Autowired
    private DiaChiGiaoHangRepository diaChiRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    public List<DiaChiGiaoHang> getDanhSachDiaChi(String email) {
        Long userId = timNguoiDung(email).getId();
        return diaChiRepository.findByNguoiDungId(userId);
    }

    @Transactional
    public DiaChiGiaoHang themDiaChi(String email, DiaChiDto request) {
        Long userId = timNguoiDung(email).getId();

        // Kiểm tra giới hạn 5 địa chỉ
        if (diaChiRepository.countByNguoiDungId(userId) >= 5) {
            throw new MaxAddressLimitException("Bạn chỉ được lưu tối đa 5 địa chỉ giao hàng!");
        }

        DiaChiGiaoHang diaChi = new DiaChiGiaoHang();
        diaChi.setNguoiDungId(userId);
        diaChi.setHoTenNguoiNhan(request.getHoTenNguoiNhan());
        diaChi.setSoDienThoaiNhan(request.getSoDienThoaiNhan());
        diaChi.setDiaChiChiTiet(request.getDiaChiChiTiet());
        diaChi.setLaMacDinh(request.getLaMacDinh());

        // Xử lý logic đặt làm mặc định
        if (Boolean.TRUE.equals(request.getLaMacDinh())) {
            gỡMặcĐịnhCũ(userId);
        }

        return diaChiRepository.save(diaChi);
    }

    @Transactional
    public DiaChiGiaoHang capNhatDiaChi(String email, Long diaChiId, DiaChiDto request) {
        Long userId = timNguoiDung(email).getId();
        DiaChiGiaoHang diaChi = diaChiRepository.findById(diaChiId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ!"));

        if (!diaChi.getNguoiDungId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền sửa địa chỉ này!");
        }

        diaChi.setHoTenNguoiNhan(request.getHoTenNguoiNhan());
        diaChi.setSoDienThoaiNhan(request.getSoDienThoaiNhan());
        diaChi.setDiaChiChiTiet(request.getDiaChiChiTiet());

        if (Boolean.TRUE.equals(request.getLaMacDinh()) && !diaChi.getLaMacDinh()) {
            gỡMặcĐịnhCũ(userId);
            diaChi.setLaMacDinh(true);
        }

        return diaChiRepository.save(diaChi);
    }

    // Đặt 1 địa chỉ đã tồn tại làm mặc định, không đụng tới các trường khác
    @Transactional
    public DiaChiGiaoHang datMacDinh(String email, Long diaChiId) {
        Long userId = timNguoiDung(email).getId();
        DiaChiGiaoHang diaChi = diaChiRepository.findById(diaChiId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ!"));

        if (!diaChi.getNguoiDungId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền sửa địa chỉ này!");
        }

        if (!Boolean.TRUE.equals(diaChi.getLaMacDinh())) {
            gỡMặcĐịnhCũ(userId);
            diaChi.setLaMacDinh(true);
            diaChiRepository.save(diaChi);
        }
        return diaChi;
    }

    public void xoaDiaChi(String email, Long diaChiId) {
        Long userId = timNguoiDung(email).getId();
        DiaChiGiaoHang diaChi = diaChiRepository.findById(diaChiId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ!"));

        if (!diaChi.getNguoiDungId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa địa chỉ này!");
        }
        diaChiRepository.delete(diaChi);
    }

    private void gỡMặcĐịnhCũ(Long userId) {
        diaChiRepository.findByNguoiDungIdAndLaMacDinhTrue(userId)
                .ifPresent(oldDefault -> {
                    oldDefault.setLaMacDinh(false);
                    diaChiRepository.save(oldDefault);
                });
    }

    private NguoiDung timNguoiDung(String email) {
        return nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));
    }
}
