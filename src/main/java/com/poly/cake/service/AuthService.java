package com.poly.cake.service;

import com.poly.cake.dto.AuthDto.*;
import com.poly.cake.entity.LamMoiToken;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.NhatKyHeThong;
import com.poly.cake.repository.LamMoiTokenRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.NhatKyHeThongRepository;
import com.poly.cake.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final NguoiDungRepository nguoiDungRepository;
    private final LamMoiTokenRepository lamMoiTokenRepository;
    private final NhatKyHeThongRepository nhatKyHeThongRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RedisTokenService redisTokenService;
    private final JavaMailSender mailSender;

    private final EmailService emailService;

    // [SỬA] Dùng SecureRandom thay cho Random để tạo OTP an toàn hơn
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // T007: Đăng ký
    @Transactional
    public void register(RegisterRequest request) {
        if (nguoiDungRepository.findByEmail(request.getEmail()).isPresent()) {
            // [GIỮ NGUYÊN] Trường hợp đăng ký báo lỗi trùng email là hợp lý
            throw new RuntimeException("Email đã được sử dụng!");
        }

        NguoiDung user = NguoiDung.builder()
                .hoTen(request.getHoTen())
                .email(request.getEmail())
                .matKhau(passwordEncoder.encode(request.getMatKhau()))
                .soDienThoai(request.getSoDienThoai())
                .quyen("KHACH_HANG")
                .trangThai("HOAT_DONG")
                .build();

        nguoiDungRepository.save(user);
    }

    // T008: Đăng nhập
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Xác thực tài khoản — Spring Security tự throw BadCredentialsException nếu sai
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMatKhau())
            );
        } catch (BadCredentialsException e) {
            // [SỬA] Không báo rõ email hay mật khẩu sai, tránh lộ thông tin
            throw new RuntimeException("Email hoặc mật khẩu không chính xác!");
        }

        NguoiDung user = nguoiDungRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        if (!user.getTrangThai().equals("HOAT_DONG")) {
            throw new RuntimeException("Tài khoản đã bị khóa hoặc ngừng hoạt động!");
        }

        // Tạo JWT
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getQuyen());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // Lưu Refresh Token vào Database (xóa token cũ trước khi cấp mới)
        lamMoiTokenRepository.deleteByNguoiDung(user);
        LamMoiToken rtEntity = LamMoiToken.builder()
                .nguoiDung(user)
                .token(refreshToken)
                .ngayHetHan(LocalDateTime.now().plusDays(7))
                .build();
        lamMoiTokenRepository.save(rtEntity);

        // Ghi nhật ký đăng nhập
        nhatKyHeThongRepository.save(NhatKyHeThong.builder()
                .nguoiDung(user)
                .hanhDong("DANG_NHAP")
                .build());

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        return response;
    }

    // T009: Làm mới Access Token (Rotate Strategy)
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        LamMoiToken savedToken = lamMoiTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh Token không hợp lệ!"));

        if (savedToken.getNgayHetHan().isBefore(LocalDateTime.now()) || !jwtUtil.isTokenValid(refreshToken)) {
            lamMoiTokenRepository.delete(savedToken);
            throw new RuntimeException("Refresh Token đã hết hạn, vui lòng đăng nhập lại!");
        }

        NguoiDung user = savedToken.getNguoiDung();
        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getQuyen());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // Rotate Refresh Token (cấp mới, xóa cũ)
        savedToken.setToken(newRefreshToken);
        savedToken.setNgayHetHan(LocalDateTime.now().plusDays(7));
        lamMoiTokenRepository.save(savedToken);

        AuthResponse response = new AuthResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        return response;
    }

    // T009: Đăng xuất
    @Transactional
    public void logout(String accessToken, String email) {
        NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Lỗi xác thực người dùng!"));

        // 1. Xóa Refresh Token trong DB
        lamMoiTokenRepository.deleteByNguoiDung(user);

        // 2. Blacklist Access Token hiện tại vào Redis
        long expirationTime = jwtUtil.getExpirationTime(accessToken);
        if (expirationTime > 0) {
            redisTokenService.blacklistToken(accessToken, expirationTime);
        }

        // 3. Ghi log
        nhatKyHeThongRepository.save(NhatKyHeThong.builder()
                .nguoiDung(user)
                .hanhDong("DANG_XUAT")
                .build());
    }

    // T010: Quên mật khẩu - Gửi mã OTP
    @Transactional
    public String forgotPassword(String email) {
        // 1. Tìm user bằng Optional. KHÔNG dùng orElseThrow() để tránh văng lỗi làm lộ thông tin.
        Optional<NguoiDung> userOpt = nguoiDungRepository.findByEmail(email);

        // 2. Chỉ thực hiện tạo và gửi OTP nếu email thực sự tồn tại dưới Database
        if (userOpt.isPresent()) {
            NguoiDung user = userOpt.get();

            // (Đoạn này giữ nguyên logic cũ của em, nhớ dùng SECURE_RANDOM ở task trước nhé)
            String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
            user.setMaOtp(otp);
            user.setOtpHetHan(LocalDateTime.now().plusMinutes(5)); // Ví dụ OTP sống 5 phút
            nguoiDungRepository.save(user);

            // Gọi service gửi email ở đây...
            emailService.sendPasswordResetOtp(user.getEmail(), otp);
        }

        // 3. POKER FACE: Luôn luôn trả về đúng 1 câu này, bất kể lệnh if ở trên có chạy hay không!
        return "Nếu email tồn tại trong hệ thống, mã OTP đã được gửi đến hộp thư của bạn.";
    }

    // T010: Đặt lại mật khẩu mới bằng OTP
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        NguoiDung user = nguoiDungRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        // [SỬA] Kiểm tra hết hạn TRƯỚC khi kiểm tra OTP
        // → Tránh lộ thông tin "OTP đúng nhưng hết hạn" vs "OTP sai"
        if (user.getOtpHetHan() == null || user.getOtpHetHan().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã OTP đã hết hạn, vui lòng yêu cầu mã mới!");
        }

        if (user.getMaOtp() == null || !user.getMaOtp().equals(request.getOtp())) {
            throw new RuntimeException("Mã OTP không chính xác!");
        }

        user.setMatKhau(passwordEncoder.encode(request.getNewPassword()));
        user.setMaOtp(null);
        user.setOtpHetHan(null);
        nguoiDungRepository.save(user);
    }
}