package com.poly.cake.service;

import com.poly.cake.dto.AuthDto.AuthResponse;
import com.poly.cake.dto.HoSoDto.ChangePasswordRequest;
import com.poly.cake.dto.HoSoDto.ProfileResponse;
import com.poly.cake.dto.HoSoDto.UpdateProfileRequest;
import com.poly.cake.entity.LamMoiToken;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.NhatKyHeThong;
import com.poly.cake.exception.BusinessException;
import com.poly.cake.exception.ResourceNotFoundException;
import com.poly.cake.repository.LamMoiTokenRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.NhatKyHeThongRepository;
import com.poly.cake.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * T066: Service cho chính người dùng đang đăng nhập tự quản lý hồ sơ của
 * mình — cập nhật thông tin cá nhân/avatar và đổi mật khẩu.
 */
@Service
@RequiredArgsConstructor
public class HoSoService {

    private final NguoiDungRepository nguoiDungRepository;
    private final LamMoiTokenRepository lamMoiTokenRepository;
    private final NhatKyHeThongRepository nhatKyHeThongRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // T066: Lấy thông tin hồ sơ cá nhân hiện tại
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        NguoiDung user = timTaiKhoan(email);
        return toResponse(user);
    }

    // T066: Cập nhật thông tin cá nhân / avatar
    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        NguoiDung user = timTaiKhoan(email);

        // Nếu đổi SĐT sang 1 số khác -> phải kiểm tra không trùng với tài khoản khác
        String soDienThoaiMoi = request.getSoDienThoai();
        if (soDienThoaiMoi != null && soDienThoaiMoi.isBlank()) {
            soDienThoaiMoi = null;
        }
        if (soDienThoaiMoi != null && !soDienThoaiMoi.equals(user.getSoDienThoai())) {
            Optional<NguoiDung> chuSoDienThoai = nguoiDungRepository.findBySoDienThoai(soDienThoaiMoi);
            if (chuSoDienThoai.isPresent() && !chuSoDienThoai.get().getId().equals(user.getId())) {
                throw new BusinessException("Số điện thoại này đã được sử dụng bởi tài khoản khác!");
            }
        }

        user.setHoTen(request.getHoTen());
        user.setSoDienThoai(soDienThoaiMoi);
        if (request.getAnhDaiDien() != null) {
            // Cho phép truyền chuỗi rỗng để xóa avatar hiện tại
            user.setAnhDaiDien(request.getAnhDaiDien().isBlank() ? null : request.getAnhDaiDien());
        }

        nguoiDungRepository.save(user);

        nhatKyHeThongRepository.save(NhatKyHeThong.builder()
                .nguoiDung(user)
                .hanhDong("CAP_NHAT_HO_SO")
                .build());

        return toResponse(user);
    }

    // T066: Đổi mật khẩu -> đăng xuất tất cả thiết bị khác, chỉ giữ phiên hiện tại
    @Transactional
    public AuthResponse changePassword(String email, ChangePasswordRequest request) {
        NguoiDung user = timTaiKhoan(email);

        // Tài khoản đăng ký qua Google/OTP SĐT không nhất thiết nhớ được
        // "mật khẩu hiện tại" do được hệ thống tự sinh ngẫu nhiên, nhưng vẫn
        // bắt buộc xác thực đúng mật khẩu cũ để tránh chiếm quyền tài khoản
        // nếu thiết bị/token bị lộ.
        if (!passwordEncoder.matches(request.getMatKhauHienTai(), user.getMatKhau())) {
            throw new BusinessException("Mật khẩu hiện tại không chính xác!");
        }

        user.setMatKhau(passwordEncoder.encode(request.getMatKhauMoi()));
        nguoiDungRepository.save(user);

        // Xóa TOÀN BỘ refresh token cũ (kể cả của thiết bị hiện tại) -> mọi
        // thiết bị khác sẽ không thể làm mới access token nữa (bị đăng xuất
        // trong tối đa 15 phút, khi access token cũ hết hạn tự nhiên).
        lamMoiTokenRepository.deleteByNguoiDung(user);

        // Cấp lại ngay 1 cặp token mới CHỈ cho thiết bị vừa đổi mật khẩu,
        // để phiên hiện tại không bị văng ra ngoài theo các thiết bị khác.
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getQuyen());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        LamMoiToken rtEntity = LamMoiToken.builder()
                .nguoiDung(user)
                .token(refreshToken)
                .ngayHetHan(LocalDateTime.now().plusDays(7))
                .build();
        lamMoiTokenRepository.save(rtEntity);

        nhatKyHeThongRepository.save(NhatKyHeThong.builder()
                .nguoiDung(user)
                .hanhDong("DOI_MAT_KHAU")
                .build());

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        return response;
    }

    private NguoiDung timTaiKhoan(String email) {
        return nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại!"));
    }

    private ProfileResponse toResponse(NguoiDung user) {
        ProfileResponse response = new ProfileResponse();
        response.setId(user.getId());
        response.setHoTen(user.getHoTen());
        // Tài khoản đăng ký qua OTP SĐT có email nội bộ (giả) do hệ thống tự
        // gán để thỏa ràng buộc DB -> không hiển thị email này cho người
        // dùng vì nó không có ý nghĩa và có thể gây hiểu nhầm.
        boolean laEmailNoiBo = user.getEmail() != null && user.getEmail().endsWith(NguoiDung.PHONE_EMAIL_SUFFIX);
        response.setEmail(laEmailNoiBo ? null : user.getEmail());
        response.setSoDienThoai(user.getSoDienThoai());
        response.setAnhDaiDien(user.getAnhDaiDien());
        response.setQuyen(user.getQuyen());
        response.setTrangThai(user.getTrangThai());
        response.setNgayTao(user.getNgayTao());
        return response;
    }
}