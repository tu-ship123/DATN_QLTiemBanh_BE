package com.poly.cake.service;

import com.poly.cake.dto.PhanCaRequest;
import com.poly.cake.entity.CaLamViec;
import com.poly.cake.entity.ChamCong;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.PhanCa;
import com.poly.cake.repository.CaLamViecRepository;
import com.poly.cake.repository.ChamCongRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.PhanCaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PhanCaService {

    @Autowired private PhanCaRepository phanCaRepository;
    @Autowired private NguoiDungRepository nguoiDungRepository;
    @Autowired private CaLamViecRepository caLamViecRepository;
    @Autowired private ChamCongRepository chamCongRepository;

    // API 1: Tạo lịch phân ca mới
    public PhanCa createPhanCa(PhanCaRequest request) {
        NguoiDung nhanVien = nguoiDungRepository.findById(request.getNhanVienId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên!"));

        CaLamViec caLamViec = caLamViecRepository.findById(request.getCaLamViecId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc!"));

        PhanCa phanCa = new PhanCa();
        phanCa.setNhanVien(nhanVien);
        phanCa.setCaLamViec(caLamViec);
        phanCa.setNgayLamViec(request.getNgayLamViec());
        phanCa.setTrangThai("DA_LAP");
        phanCa.setGhiChu(request.getGhiChu());

        return phanCaRepository.save(phanCa);
    }

    // API 2: Lấy bảng chấm công tổng hợp
    public List<ChamCong> getAllChamCong() {
        return chamCongRepository.findAll();
    }
}