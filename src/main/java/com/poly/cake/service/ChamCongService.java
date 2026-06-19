package com.poly.cake.service;

import com.poly.cake.entity.ChamCong;
import com.poly.cake.entity.PhanCa;
import com.poly.cake.repository.ChamCongRepository;
import com.poly.cake.repository.PhanCaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Service
public class ChamCongService {

    @Autowired private PhanCaRepository phanCaRepository;
    @Autowired private ChamCongRepository chamCongRepository;

    // 1. Logic xử lý Check-in (Vào ca)
    public ChamCong checkIn(Long nhanVienId) {
        LocalDate homNay = LocalDate.now();

        // Tìm lịch phân ca của nhân viên trong ngày hôm nay
        PhanCa phanCa = phanCaRepository.findAll().stream()
                .filter(pc -> pc.getNhanVien().getId().equals(nhanVienId)
                        && pc.getNgayLamViec().equals(homNay)
                        && "DA_LAP".equals(pc.getTrangThai()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Hôm nay bạn không có lịch phân ca làm việc!"));

        // Tránh trường hợp một ca làm việc bấm Check-in 2 lần
        if (chamCongRepository.findByPhanCa(phanCa).isPresent()) {
            throw new RuntimeException("Bạn đã check-in ca làm việc này rồi!");
        }

        LocalTime gioHienTai = LocalTime.now();
        LocalTime gioBatDauCa = phanCa.getCaLamViec().getGioBatDau();

        ChamCong chamCong = new ChamCong();
        chamCong.setPhanCa(phanCa);
        chamCong.setGioVao(LocalDateTime.now());

        // Thuật toán kiểm tra đi muộn
        if (gioHienTai.isAfter(gioBatDauCa)) {
            long phutTre = ChronoUnit.MINUTES.between(gioBatDauCa, gioHienTai);
            chamCong.setPhutDiTre((int) phutTre);
            chamCong.setTrangThai("DI_TRE");
        } else {
            chamCong.setPhutDiTre(0);
            chamCong.setTrangThai("DUNG_GIO");
        }

        return chamCongRepository.save(chamCong);
    }

    // 2. Logic xử lý Check-out (Ra ca)
    public ChamCong checkOut(Long nhanVienId) {
        LocalDate homNay = LocalDate.now();

        PhanCa phanCa = phanCaRepository.findAll().stream()
                .filter(pc -> pc.getNhanVien().getId().equals(nhanVienId) && pc.getNgayLamViec().equals(homNay))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch phân ca hôm nay để tiến hành ra ca!"));

        ChamCong chamCong = chamCongRepository.findByPhanCa(phanCa)
                .orElseThrow(() -> new RuntimeException("Bạn chưa bấm Check-in đầu giờ, không thể Check-out!"));

        if (chamCong.getGioRa() != null) {
            throw new RuntimeException("Bạn đã check-out ca làm việc này rồi!");
        }

        chamCong.setGioRa(LocalDateTime.now());

        // Kiểm tra xem nhân viên có về sớm hơn giờ kết thúc ca hay không
        LocalTime gioHienTai = LocalTime.now();
        LocalTime gioKetThucCa = phanCa.getCaLamViec().getGioKetThuc();
        if (gioHienTai.isBefore(gioKetThucCa)) {
            chamCong.setTrangThai("VE_SOM");
        }

        return chamCongRepository.save(chamCong);
    }
}