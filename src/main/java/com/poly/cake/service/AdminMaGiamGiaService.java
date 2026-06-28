package com.poly.cake.service;

import com.poly.cake.dto.MaGiamGiaDto;
import com.poly.cake.entity.MaGiamGia;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.repository.MaGiamGiaRepository;
import com.poly.cake.repository.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminMaGiamGiaService {

    @Autowired
    private MaGiamGiaRepository maGiamGiaRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private EmailService emailService;

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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã giảm giá"));
        return mapToDto(voucher);
    }

    // CREATE
    @Transactional
    public MaGiamGiaDto.Response create(MaGiamGiaDto.Request request) {
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
        voucher.setDiemCanDung(request.getDiemCanDung()); // ← thêm

        return mapToDto(maGiamGiaRepository.save(voucher));
    }

    // UPDATE
    @Transactional
    public MaGiamGiaDto.Response update(Long id, MaGiamGiaDto.Request request) {
        MaGiamGia voucher = maGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã giảm giá"));

        voucher.setMaCode(request.getMaCode().toUpperCase());
        voucher.setLoaiGiamGia(request.getLoaiGiamGia());
        voucher.setGiaTriGiam(request.getGiaTriGiam());
        voucher.setDonHangToiThieu(request.getDonHangToiThieu());
        voucher.setSoLuotToiDa(request.getSoLuotToiDa());
        voucher.setNgayHetHan(request.getNgayHetHan());
        voucher.setHoatDong(request.getHoatDong());
        voucher.setDiemCanDung(request.getDiemCanDung()); // ← thêm

        return mapToDto(maGiamGiaRepository.save(voucher));
    }

    // GỬI EMAIL KHUYẾN MÃI ĐẾN TẤT CẢ KHÁCH HÀNG
    @Transactional(readOnly = true)
    public int sendPromoEmailToAllCustomers(Long voucherId) {
        MaGiamGia voucher = maGiamGiaRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã giảm giá"));

        if (!Boolean.TRUE.equals(voucher.getHoatDong())) {
            throw new RuntimeException("Voucher chưa được kích hoạt, không thể gửi email");
        }
        if (voucher.getNgayHetHan().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Voucher đã hết hạn, không thể gửi email");
        }

        List<NguoiDung> khachHangList = nguoiDungRepository.findAll()
                .stream()
                .filter(nd -> "KHACH_HANG".equals(nd.getQuyen())
                        && "HOAT_DONG".equals(nd.getTrangThai()))
                .collect(Collectors.toList());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String ngayHetHan = voucher.getNgayHetHan().format(fmt);
        String giaTriGiam = voucher.getGiaTriGiam().toPlainString();
        String donHangToiThieu = voucher.getDonHangToiThieu() != null
                ? voucher.getDonHangToiThieu().toPlainString() : "0";

        int soEmailDaGui = 0;
        for (NguoiDung kh : khachHangList) {
            try {
                emailService.sendPromoVoucherEmail(
                        kh.getEmail(),
                        kh.getHoTen(),
                        voucher.getMaCode(),
                        voucher.getLoaiGiamGia(),
                        giaTriGiam,
                        ngayHetHan,
                        donHangToiThieu
                );
                soEmailDaGui++;
            } catch (Exception e) {
                // Bỏ qua email lỗi, tiếp tục gửi cho người khác
            }
        }
        return soEmailDaGui;
    }

    // DELETE
    @Transactional
    public void delete(Long id) {
        MaGiamGia voucher = maGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã giảm giá"));
        maGiamGiaRepository.delete(voucher);
    }

    private MaGiamGiaDto.Response mapToDto(MaGiamGia voucher) {
        MaGiamGiaDto.Response dto = new MaGiamGiaDto.Response();
        dto.setId(voucher.getId());
        dto.setMaCode(voucher.getMaCode());
        dto.setLoaiGiamGia(voucher.getLoaiGiamGia());
        dto.setGiaTriGiam(voucher.getGiaTriGiam());
        dto.setDonHangToiThieu(voucher.getDonHangToiThieu());
        dto.setSoLuotToiDa(voucher.getSoLuotToiDa());
        dto.setSoLuotDaDung(voucher.getSoLuotDaDung());
        dto.setNgayHetHan(voucher.getNgayHetHan());
        dto.setHoatDong(voucher.getHoatDong());
        dto.setDiemCanDung(voucher.getDiemCanDung()); // ← thêm
        return dto;
    }
}