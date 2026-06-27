package com.poly.cake.service;

import com.poly.cake.dto.MaGiamGiaDto;
import com.poly.cake.entity.MaGiamGia;
import com.poly.cake.repository.MaGiamGiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminMaGiamGiaService {

    @Autowired
    private MaGiamGiaRepository maGiamGiaRepository;

    // GET ALL
    public List<MaGiamGiaDto.Response> getAll() {

        return maGiamGiaRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // GET BY ID
    public MaGiamGiaDto.Response getById(Long id) {

        MaGiamGia voucher = maGiamGiaRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy mã giảm giá"));

        return mapToDto(voucher);
    }

    // CREATE
    @Transactional
    public MaGiamGiaDto.Response create(
            MaGiamGiaDto.Request request) {

        if (maGiamGiaRepository.existsByMaCode(request.getMaCode())) {
            throw new RuntimeException("Mã giảm giá đã tồn tại");
        }

        if (request.getNgayHetHan().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Ngày hết hạn phải lớn hơn hiện tại");
        }

        MaGiamGia voucher = new MaGiamGia();

        voucher.setMaCode(request.getMaCode().toUpperCase());
        voucher.setLoaiGiamGia(request.getLoaiGiamGia());
        voucher.setGiaTriGiam(request.getGiaTriGiam());
        voucher.setDonHangToiThieu(request.getDonHangToiThieu());
        voucher.setSoLuotToiDa(request.getSoLuotToiDa());
        voucher.setNgayHetHan(request.getNgayHetHan());
        voucher.setHoatDong(request.getHoatDong());

        return mapToDto(
                maGiamGiaRepository.save(voucher)
        );
    }

    // UPDATE
    @Transactional
    public MaGiamGiaDto.Response update(
            Long id,
            MaGiamGiaDto.Request request) {

        MaGiamGia voucher = maGiamGiaRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy mã giảm giá"));

        boolean trungVoiMaKhac = maGiamGiaRepository.existsByMaCode(request.getMaCode().toUpperCase())
                && !voucher.getMaCode().equalsIgnoreCase(request.getMaCode());
        if (trungVoiMaKhac) {
            throw new RuntimeException("Mã giảm giá " + request.getMaCode() + " đã được sử dụng!");
        }

        voucher.setMaCode(request.getMaCode().toUpperCase());
        voucher.setLoaiGiamGia(request.getLoaiGiamGia());
        voucher.setGiaTriGiam(request.getGiaTriGiam());
        voucher.setDonHangToiThieu(request.getDonHangToiThieu());
        voucher.setSoLuotToiDa(request.getSoLuotToiDa());
        voucher.setNgayHetHan(request.getNgayHetHan());
        voucher.setHoatDong(request.getHoatDong());

        return mapToDto(
                maGiamGiaRepository.save(voucher)
        );
    }

    // DELETE
    @Transactional
    public void delete(Long id) {

        MaGiamGia voucher = maGiamGiaRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy mã giảm giá"));

        maGiamGiaRepository.delete(voucher);
    }

    private MaGiamGiaDto.Response mapToDto(
            MaGiamGia voucher) {

        MaGiamGiaDto.Response dto =
                new MaGiamGiaDto.Response();

        dto.setId(voucher.getId());
        dto.setMaCode(voucher.getMaCode());
        dto.setLoaiGiamGia(voucher.getLoaiGiamGia());
        dto.setGiaTriGiam(voucher.getGiaTriGiam());
        dto.setDonHangToiThieu(voucher.getDonHangToiThieu());
        dto.setSoLuotToiDa(voucher.getSoLuotToiDa());
        dto.setSoLuotDaDung(voucher.getSoLuotDaDung());
        dto.setNgayHetHan(voucher.getNgayHetHan());
        dto.setHoatDong(voucher.getHoatDong());

        return dto;
    }
}