package com.poly.cake.service;

import com.poly.cake.dto.StaffDto;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.repository.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j; // Import thư viện này

@Slf4j
@Service
public class StaffService {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;
    // 1. Lấy danh sách nhân viên
    public List<NguoiDung> getAllStaffs() {
        // Giả sử trong DB, quyền nhân viên là "NHAN_VIEN"
        return nguoiDungRepository.findAll().stream()
                .filter(nd -> "NHAN_VIEN".equals(nd.getQuyen()))
                .toList();
    }

    public NguoiDung createStaff(StaffDto.CreateRequest request) {
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên có ID: " + id));

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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên!"));
        existingStaff.setTrangThai("BI_KHOA");// 0: Khóa tài khoản
        nguoiDungRepository.save(existingStaff);
    }
}