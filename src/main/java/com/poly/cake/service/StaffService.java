package com.poly.cake.service;

import com.poly.cake.entity.NguoiDung;
import com.poly.cake.repository.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

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

    // 2. Thêm mới nhân viên
    public NguoiDung createStaff(NguoiDung staff) {
        if (nguoiDungRepository.findByEmail(staff.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống!");
        }

        // Tạo mật khẩu ngẫu nhiên 8 ký tự
        String randomPassword = UUID.randomUUID().toString().substring(0, 8);
        System.out.println("Mật khẩu cấp cho " + staff.getEmail() + " là: " + randomPassword);

        staff.setMatKhau(passwordEncoder.encode(randomPassword));
        staff.setQuyen("NHAN_VIEN");
        staff.setTrangThai("HOAT_DONG"); // 1: Hoạt động

        NguoiDung savedStaff = nguoiDungRepository.save(staff);

        System.out.println("Bắt đầu gửi email cho: " + staff.getEmail());
        emailService.sendNewStaffEmail(staff.getEmail(), staff.getHoTen(), randomPassword);
        System.out.println("Đã gửi email thành công!");
        return savedStaff;
    }

    // 3. Cập nhật thông tin nhân viên
    public NguoiDung updateStaff(Long id, NguoiDung staffDetails) {
        NguoiDung existingStaff = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên!"));

        existingStaff.setHoTen(staffDetails.getHoTen());
        existingStaff.setSoDienThoai(staffDetails.getSoDienThoai());
        existingStaff.setTrangThai(staffDetails.getTrangThai());

        return nguoiDungRepository.save(existingStaff);
    }

    // 4. Khóa/Xóa nhân viên (Soft Delete - Đổi trạng thái)
    public void deleteStaff(Long id) {
        NguoiDung existingStaff = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên!"));
        existingStaff.setTrangThai("BI_KHOA");// 0: Khóa tài khoản
        nguoiDungRepository.save(existingStaff);
    }
}