package com.poly.cake.service;

import com.poly.cake.exception.BusinessException;
import com.poly.cake.exception.ResourceNotFoundException;

import com.poly.cake.dto.NhanVienHieuSuatDto;
import com.poly.cake.dto.StaffDto;
import com.poly.cake.dto.HieuSuatNhanVienDto;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.repository.DonHangRepository;
import com.poly.cake.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j; // Import thư viện này

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

    private final NguoiDungRepository nguoiDungRepository;
    private final DonHangRepository donHangRepository;

    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    // 1. Lấy danh sách nhân viên
    public List<NguoiDung> getAllStaffs() {
        // Giả sử trong DB, quyền nhân viên là "NHAN_VIEN"
        return nguoiDungRepository.findAll().stream()
                .filter(nd -> "NHAN_VIEN".equals(nd.getQuyen()))
                .toList();
    }

    public NguoiDung createStaff(StaffDto.CreateRequest request) {
        // Điều kiện 1: Email đã tồn tại (dù là khách hàng, nhân viên hay admin) -> chặn sớm,
        // tránh để văng lỗi 500 do vi phạm ràng buộc UNIQUE ở DB (khó hiểu với FE)
        if (nguoiDungRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email đã được sử dụng, vui lòng chọn email khác!");
        }

        // Điều kiện 2: Số điện thoại đã tồn tại -> báo lỗi rõ ràng
        if (request.getSoDienThoai() != null && !request.getSoDienThoai().isBlank()
                && nguoiDungRepository.existsBySoDienThoai(request.getSoDienThoai())) {
            throw new BusinessException("Số điện thoại đã được sử dụng, vui lòng chọn số khác!");
        }

        // 1. Khởi tạo một Entity mới cứng
        NguoiDung staff = new NguoiDung();

        // 2. Chỉ nhặt những thông tin được phép từ DTO sang
        staff.setHoTen(request.getHoTen());
        staff.setEmail(request.getEmail());
        staff.setSoDienThoai(request.getSoDienThoai());

        // 3. Hệ thống tự hard-code các trường nhạy cảm, tuyệt đối không cho bên ngoài can thiệp
        staff.setQuyen("NHAN_VIEN");
        staff.setTrangThai("HOAT_DONG");
        // Giả sử em có tiêm passwordEncoder vào Service rồi nhé
        staff.setMatKhau(passwordEncoder.encode("123456"));
        staff.setNgayTao(LocalDateTime.now());

        return nguoiDungRepository.save(staff);
    }

    public NguoiDung updateStaff(Long id, StaffDto.UpdateRequest request) {
        NguoiDung staff = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên có ID: " + id));

        // Chỉ cập nhật những trường cho phép
        staff.setHoTen(request.getHoTen());
        staff.setSoDienThoai(request.getSoDienThoai());

        // Trạng thái có thể được Admin cập nhật (Ví dụ: Khóa tài khoản)
        if (request.getTrangThai() != null && !request.getTrangThai().isEmpty()) {
            staff.setTrangThai(request.getTrangThai());
        }

        return nguoiDungRepository.save(staff);
    }
    // 4. Khóa/Xóa nhân viên (Soft Delete - Đổi trạng thái)
    public void deleteStaff(Long id) {
        NguoiDung existingStaff = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên!"));
        existingStaff.setTrangThai("BI_KHOA");// 0: Khóa tài khoản
        nguoiDungRepository.save(existingStaff);
    }

    // 5. Hiệu suất nhân viên (Admin) - GET /api/v1/admin/staff/hieu-suat
    // Ghép danh sách toàn bộ nhân viên với số đơn xử lý + doanh thu trong khoảng thời gian (nếu có),
    // rồi tính % hiệu suất tương đối so với nhân viên có doanh thu cao nhất.
    public List<NhanVienHieuSuatDto> getHieuSuat(LocalDateTime tuNgay, LocalDateTime denNgay) {
        List<NguoiDung> nhanViens = nguoiDungRepository.findByQuyen("NHAN_VIEN");

        List<HieuSuatNhanVienDto> thongKe = donHangRepository.getHieuSuatNhanVienTheoKhoang(tuNgay, denNgay);
        Map<Long, HieuSuatNhanVienDto> thongKeMap = thongKe.stream()
                .collect(Collectors.toMap(HieuSuatNhanVienDto::getNhanVienId, dto -> dto));

        BigDecimal doanhThuCaoNhat = thongKe.stream()
                .map(HieuSuatNhanVienDto::getTongDoanhThu)
                .filter(java.util.Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return nhanViens.stream().map(nv -> {
            HieuSuatNhanVienDto tk = thongKeMap.get(nv.getId());
            BigDecimal doanhThu = tk != null && tk.getTongDoanhThu() != null ? tk.getTongDoanhThu() : BigDecimal.ZERO;
            Long soDon = tk != null ? tk.getTongSoDon() : 0L;

            int hieuSuat = 0;
            if (doanhThuCaoNhat.compareTo(BigDecimal.ZERO) > 0) {
                hieuSuat = doanhThu.multiply(BigDecimal.valueOf(100))
                        .divide(doanhThuCaoNhat, 0, java.math.RoundingMode.HALF_UP)
                        .intValue();
            }

            return new NhanVienHieuSuatDto(
                    nv.getId(),
                    nv.getHoTen(),
                    nv.getEmail(),
                    "HOAT_DONG".equals(nv.getTrangThai()),
                    soDon,
                    doanhThu,
                    hieuSuat
            );
        }).sorted((a, b) -> b.getDoanhThu().compareTo(a.getDoanhThu())).collect(Collectors.toList());
    }
}