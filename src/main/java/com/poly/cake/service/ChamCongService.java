package com.poly.cake.service;

import com.poly.cake.dto.StaffCheckinRequest;
import com.poly.cake.dto.ChamCongResponse;
import com.poly.cake.entity.ChamCong;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.PhanCa;
import com.poly.cake.repository.ChamCongRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.PhanCaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ChamCongService {

    private final ChamCongRepository chamCongRepository;
    private final PhanCaRepository phanCaRepository;
    private final NguoiDungRepository nguoiDungRepository;

    // Lấy nhân viên đang đăng nhập
    private NguoiDung getNhanVienHienTai() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    /**
     * CHECK-IN: Nhân viên chọn ca đã phân, ghi nhận giờ vào, tính phút đi trễ
     */
    @Transactional
    public ChamCongResponse checkIn(StaffCheckinRequest request) {
        System.out.println("DEBUG: phanCaId từ request là: " + request.getPhanCaId());

        NguoiDung nhanVien = getNhanVienHienTai();
        System.out.println("DEBUG: ID nhân viên hiện tại là: " + nhanVien.getId());
        // ------------------------------

        // Tìm phân ca theo id và đảm bảo là ca của nhân viên này
        PhanCa phanCa = phanCaRepository.findByIdAndNhanVienId(request.getPhanCaId(), nhanVien.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phân ca hoặc không có quyền truy cập"));

        // Kiểm tra trạng thái phân ca
        if ("DA_HUY".equals(phanCa.getTrangThai())) {
            throw new RuntimeException("Ca làm việc này đã bị hủy");
        }

        // Kiểm tra đã check-in chưa
        if (chamCongRepository.existsByPhanCa(phanCa)) {
            throw new RuntimeException("Bạn đã check-in ca này rồi");
        }

        LocalDateTime gioVaoThucTe = LocalDateTime.now();
        LocalTime gioBatDauCa = phanCa.getCaLamViec().getGioBatDau();
        LocalTime gioVaoTime = gioVaoThucTe.toLocalTime();

        // Tính phút đi trễ
        int phutDiTre = 0;
        String trangThai = "DUNG_GIO";

        if (gioVaoTime.isAfter(gioBatDauCa)) {
            phutDiTre = (int) ChronoUnit.MINUTES.between(gioBatDauCa, gioVaoTime);
            trangThai = "DI_TRE";
        }

        ChamCong chamCong = ChamCong.builder()
                .phanCa(phanCa)
                .gioVao(gioVaoThucTe)
                .phutDiTre(phutDiTre)
                .trangThai(trangThai)
                .build();

        chamCong = chamCongRepository.save(chamCong);

        // Cập nhật trạng thái phân ca thành XAC_NHAN
        phanCa.setTrangThai("XAC_NHAN");
        phanCaRepository.save(phanCa);

        return mapToResponse(chamCong);
    }

    /**
     * CHECK-OUT: Chốt giờ ra, cập nhật trạng thái nếu về sớm
     */

    @Transactional
    public ChamCongResponse checkOut(Long phanCaId) {

        NguoiDung nhanVien = getNhanVienHienTai();

        PhanCa phanCa = phanCaRepository.findByIdAndNhanVienId(phanCaId, nhanVien.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phân ca hoặc không có quyền truy cập"));

        ChamCong chamCong = chamCongRepository.findByPhanCa(phanCa)
                .orElseThrow(() -> new RuntimeException("Bạn chưa check-in ca này"));

        if (chamCong.getGioRa() != null) {
            throw new RuntimeException("Bạn đã check-out ca này rồi");
        }

        LocalDateTime gioRaThucTe = LocalDateTime.now();
        LocalTime gioKetThucCa = phanCa.getCaLamViec().getGioKetThuc();
        LocalTime gioRaTime = gioRaThucTe.toLocalTime();

        chamCong.setGioRa(gioRaThucTe);

        // Kiểm tra về sớm
        if (gioRaTime.isBefore(gioKetThucCa) && "DUNG_GIO".equals(chamCong.getTrangThai())) {
            chamCong.setTrangThai("VE_SOM");
        }

        chamCong = chamCongRepository.save(chamCong);
        return mapToResponse(chamCong);
    }

    private ChamCongResponse mapToResponse(ChamCong cc) {
        return ChamCongResponse.builder()
                .id(cc.getId())
                .phanCaId(cc.getPhanCa().getId())
                .tenCa(cc.getPhanCa().getCaLamViec().getTenCa())
                .ngayLamViec(cc.getPhanCa().getNgayLamViec().toString())
                .gioVao(cc.getGioVao())
                .gioRa(cc.getGioRa())
                .phutDiTre(cc.getPhutDiTre())
                .trangThai(cc.getTrangThai())
                .build();
    }
}