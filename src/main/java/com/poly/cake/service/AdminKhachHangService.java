package com.poly.cake.service;

import com.poly.cake.exception.BusinessException;
import com.poly.cake.exception.ResourceNotFoundException;
import com.poly.cake.exception.ForbiddenException;

import com.poly.cake.dto.KhachHangDto;
import com.poly.cake.entity.DiemThuong;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.repository.DiemThuongRepository;
import com.poly.cake.repository.DonHangRepository;
import com.poly.cake.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminKhachHangService {

    private final NguoiDungRepository nguoiDungRepository;

    private final DiemThuongRepository diemThuongRepository;

    private final DonHangRepository donHangRepository;

    // GET ALL khách hàng
    public List<KhachHangDto.Response> getAll() {
        return nguoiDungRepository.findAll()
                .stream()
                .filter(nd -> "KHACH_HANG".equals(nd.getQuyen()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // GET BY ID
    public KhachHangDto.Response getById(Long id) {
        NguoiDung kh = findKhachHang(id);
        return mapToDto(kh);
    }

    // UPDATE thông tin cơ bản (họ tên, SĐT, trạng thái)
    @Transactional
    public KhachHangDto.Response update(Long id, KhachHangDto.UpdateRequest req) {
        NguoiDung kh = findKhachHang(id);
        kh.setHoTen(req.getHoTen());
        kh.setSoDienThoai(req.getSoDienThoai());
        kh.setTrangThai(req.getTrangThai());
        return mapToDto(nguoiDungRepository.save(kh));
    }

    // KHÓA / MỞ KHÓA tài khoản
    @Transactional
    public KhachHangDto.Response toggleTrangThai(Long id) {
        NguoiDung kh = findKhachHang(id);
        if ("HOAT_DONG".equals(kh.getTrangThai())) {
            kh.setTrangThai("BI_KHOA");
        } else {
            kh.setTrangThai("HOAT_DONG");
        }
        return mapToDto(nguoiDungRepository.save(kh));
    }

    // ĐIỀU CHỈNH ĐIỂM THỦ CÔNG
    @Transactional
    public KhachHangDto.Response adjustDiem(Long id, KhachHangDto.AdjustPointRequest req) {
        NguoiDung kh = findKhachHang(id);

        // Kiểm tra nếu trừ điểm, khách có đủ không
        if (req.getDiemThayDoi() < 0) {
            Integer tongDiem = diemThuongRepository.tinhTongDiem(kh);
            if (tongDiem + req.getDiemThayDoi() < 0) {
                throw new BusinessException("Khách không đủ điểm để trừ. Điểm hiện có: " + tongDiem);
            }
        }

        DiemThuong giaoDich = new DiemThuong();
        giaoDich.setKhachHang(kh);
        giaoDich.setDiemThayDoi(req.getDiemThayDoi());
        giaoDich.setLoaiGiaoDich("ADMIN_CHINH_SUA");
        giaoDich.setMoTa(req.getMoTa());
        diemThuongRepository.save(giaoDich);

        return mapToDto(kh);
    }

    // DELETE khách hàng
    @Transactional
    public void delete(Long id) {
        NguoiDung kh = findKhachHang(id);
        nguoiDungRepository.delete(kh);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private NguoiDung findKhachHang(Long id) {
        NguoiDung kh = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với id=" + id));
        if (!"KHACH_HANG".equals(kh.getQuyen())) {
            throw new ForbiddenException("Người dùng này không phải khách hàng");
        }
        return kh;
    }

    private KhachHangDto.Response mapToDto(NguoiDung kh) {
        KhachHangDto.Response dto = new KhachHangDto.Response();
        dto.setId(kh.getId());
        dto.setHoTen(kh.getHoTen());
        dto.setEmail(kh.getEmail());
        dto.setSoDienThoai(kh.getSoDienThoai());
        dto.setAnhDaiDien(kh.getAnhDaiDien());
        dto.setTrangThai(kh.getTrangThai());
        dto.setNgayTao(kh.getNgayTao());

        // Điểm tích lũy
        Integer diem = diemThuongRepository.tinhTongDiem(kh);
        dto.setTongDiem(diem != null ? diem : 0);

        // Tổng đơn hàng
        long tongDon = donHangRepository.findByKhachHangOrderByNgayTaoDesc(kh).size();
        dto.setTongDonHang(tongDon);

        // Tổng chi tiêu (không tính đơn hủy)
        BigDecimal tongChiTieu = donHangRepository.findByKhachHangOrderByNgayTaoDesc(kh)
                .stream()
                .filter(d -> !"DA_HUY".equals(d.getTrangThai()))
                .map(d -> d.getTongTien() != null ? d.getTongTien() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        dto.setTongChiTieu(nf.format(tongChiTieu) + "đ");

        return dto;
    }
}