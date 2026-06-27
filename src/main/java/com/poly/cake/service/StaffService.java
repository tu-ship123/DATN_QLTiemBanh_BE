package com.poly.cake.service;

import com.poly.cake.dto.StaffDto;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.exception.ResourceNotFoundException;
import com.poly.cake.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // --- CẤU HÌNH BỘ SINH MẬT KHẨU NGẪU NHIÊN ---
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#!";

    private String generateTempPassword() {
        return SECURE_RANDOM.ints(12, 0, CHARS.length())
                .mapToObj(i -> String.valueOf(CHARS.charAt(i)))
                .collect(Collectors.joining());
    }
    // ---------------------------------------------

    // 1. Lấy danh sách nhân viên
    public List<StaffDto.Response> getAllStaffs() {
        return nguoiDungRepository.findByQuyen("NHAN_VIEN").stream()
                .map(this::mapToResponse) // Tái sử dụng hàm mapToResponse cho code gọn gàng
                .toList();
    }

    // 2. Thêm mới nhân viên (ĐÃ FIX BUG SỐ 8: Trả về DTO)
    public StaffDto.Response createStaff(StaffDto.CreateRequest request) {
        NguoiDung staff = new NguoiDung();

        staff.setHoTen(request.getHoTen());
        staff.setEmail(request.getEmail());
        staff.setSoDienThoai(request.getSoDienThoai());

        staff.setQuyen("NHAN_VIEN");
        staff.setTrangThai("HOAT_DONG");
        staff.setNgayTao(LocalDateTime.now());

        // Sinh mật khẩu ngẫu nhiên & mã hóa
        String tempPassword = generateTempPassword();
        staff.setMatKhau(passwordEncoder.encode(tempPassword));

        // Lưu xuống Database
        NguoiDung savedStaff = nguoiDungRepository.save(staff);

        // Gửi email mật khẩu tạm thời cho nhân viên mới
        try {
            emailService.sendTempPasswordToStaff(savedStaff.getEmail(), tempPassword);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email mật khẩu cho nhân viên {}: {}", savedStaff.getEmail(), e.getMessage());
        }

        return mapToResponse(savedStaff);
    }

    // 3. Cập nhật thông tin nhân viên (ĐÃ FIX BUG SỐ 8: Trả về DTO)
    public StaffDto.Response updateStaff(Long id, StaffDto.UpdateRequest request) {
        NguoiDung staff = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên có ID: " + id));

        staff.setHoTen(request.getHoTen());
        staff.setSoDienThoai(request.getSoDienThoai());

        if (request.getTrangThai() != null && !request.getTrangThai().isEmpty()) {
            staff.setTrangThai(request.getTrangThai());
        }

        NguoiDung updatedStaff = nguoiDungRepository.save(staff);

        return mapToResponse(updatedStaff);
    }

    // 4. Khóa/Xóa nhân viên (Soft Delete)
    public void deleteStaff(Long id) {
        NguoiDung existingStaff = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên có ID: " + id));

        existingStaff.setTrangThai("BI_KHOA");
        nguoiDungRepository.save(existingStaff);
    }

    // --- HÀM PHỤ TRỢ ---
    // Dùng để nhặt các thông tin an toàn trả về cho Frontend, che giấu mật khẩu và dữ liệu nhạy cảm
    private StaffDto.Response mapToResponse(NguoiDung nd) {
        return new StaffDto.Response(
                nd.getId(),
                nd.getHoTen(),
                nd.getEmail(),
                nd.getSoDienThoai(),
                nd.getTrangThai()
        );
    }
}