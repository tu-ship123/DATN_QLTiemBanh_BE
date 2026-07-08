package com.poly.cake.service;

import com.poly.cake.exception.BusinessException;
import com.poly.cake.exception.ResourceNotFoundException;

import com.poly.cake.dto.AuthDto.*;
import com.poly.cake.entity.LamMoiToken;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.NhatKyHeThong;
import com.poly.cake.repository.LamMoiTokenRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.NhatKyHeThongRepository;
import com.poly.cake.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
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
    private final TotpService totpService;
    private final JavaMailSender mailSender;
    private final EmailService emailService;
    private final SmsService smsService;
    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${google.client-id}")
    private String googleClientId;

    // Hậu tố email giả (nội bộ) cấp cho tài khoản đăng ký bằng OTP SĐT, vì
    // cột email trong DB đang là NOT NULL UNIQUE và toàn bộ hệ thống hiện
    // dùng email làm định danh đăng nhập (JWT subject). Email này KHÔNG gửi
    // cho người dùng và không dùng để đăng nhập bằng mật khẩu.
    private static final String PHONE_EMAIL_SUFFIX = "@phone.chocopine.local";

    // Tiền tố key Redis lưu OTP đăng ký SĐT, TTL 5 phút
    private static final String OTP_REGISTER_PREFIX = "otp:register:";
    private static final long OTP_TTL_MINUTES = 5;
    // Giới hạn gửi lại OTP: tối thiểu 60 giây / lần để tránh spam SMS
    private static final long OTP_RESEND_COOLDOWN_SECONDS = 60;

    // [SỬA] Dùng SecureRandom thay cho Random để tạo OTP an toàn hơn
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // T007: Đăng ký
    @Transactional
    public void register(RegisterRequest request) {
        // Điều kiện 1: Email đã tồn tại -> không cho đăng ký trùng
        if (nguoiDungRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email đã được sử dụng!");
        }

        // Điều kiện 2: Số điện thoại đã được dùng cho tài khoản khác -> báo lỗi rõ ràng cho FE
        if (request.getSoDienThoai() != null && !request.getSoDienThoai().isBlank()
                && nguoiDungRepository.existsBySoDienThoai(request.getSoDienThoai())) {
            throw new BusinessException("Số điện thoại đã được sử dụng!");
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
            throw new BusinessException("Email hoặc mật khẩu không chính xác!");
        }

        NguoiDung user = nguoiDungRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));

        // Điều kiện trạng thái tài khoản: áp dụng chung cho cả khách hàng & nhân viên
        // vì hai đối tượng này dùng chung 1 endpoint đăng nhập, chỉ khác nhau ở field "quyen"
        if ("BI_KHOA".equals(user.getTrangThai())) {
            throw new BusinessException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên để được hỗ trợ!");
        }
        if ("NGUNG_HOAT_DONG".equals(user.getTrangThai())) {
            throw new BusinessException("Tài khoản của bạn đã ngừng hoạt động!");
        }
        if (!"HOAT_DONG".equals(user.getTrangThai())) {
            throw new BusinessException("Tài khoản không ở trạng thái hoạt động, vui lòng liên hệ quản trị viên!");
        }

        // Xử lý 2FA (TOTP)
        if (Boolean.TRUE.equals(user.getIs2FaEnabled())) {
            if (request.getTotpCode() == null || request.getTotpCode().isBlank()) {
                throw new BusinessException("Vui lòng nhập mã xác thực 2 bước (2FA)!");
            }
            if (!totpService.verifyCode(user.getTotpSecret(), request.getTotpCode())) {
                throw new BusinessException("Mã xác thực 2 bước không chính xác!");
            }
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
                .orElseThrow(() -> new BusinessException("Refresh Token không hợp lệ!"));

        if (savedToken.getNgayHetHan().isBefore(LocalDateTime.now()) || !jwtUtil.isTokenValid(refreshToken)) {
            lamMoiTokenRepository.delete(savedToken);
            throw new BusinessException("Refresh Token đã hết hạn, vui lòng đăng nhập lại!");
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
                .orElseThrow(() -> new BusinessException("Lỗi xác thực người dùng!"));

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

            // Gửi OTP qua email
            emailService.sendPasswordResetOtp(user.getEmail(), otp);
        }

        // 3. POKER FACE: Luôn luôn trả về đúng 1 câu này, bất kể lệnh if ở trên có chạy hay không!
        return "Nếu email tồn tại trong hệ thống, mã OTP đã được gửi đến hộp thư của bạn.";
    }

    // T010: Đặt lại mật khẩu mới bằng OTP
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        NguoiDung user = nguoiDungRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại!"));

        // [SỬA] Kiểm tra hết hạn TRƯỚC khi kiểm tra OTP
        // → Tránh lộ thông tin "OTP đúng nhưng hết hạn" vs "OTP sai"
        if (user.getOtpHetHan() == null || user.getOtpHetHan().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Mã OTP đã hết hạn, vui lòng yêu cầu mã mới!");
        }

        if (user.getMaOtp() == null || !user.getMaOtp().equals(request.getOtp())) {
            throw new BusinessException("Mã OTP không chính xác!");
        }

        user.setMatKhau(passwordEncoder.encode(request.getNewPassword()));
        user.setMaOtp(null);
        user.setOtpHetHan(null);
        nguoiDungRepository.save(user);
    }

    // ═══════════════════════════════════════════════════════════════════
    // T065 - Bước 1: Gửi mã OTP đăng ký bằng số điện thoại
    // ═══════════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public void sendRegisterOtp(SendPhoneOtpRequest request) {
        String phone = request.getSoDienThoai();

        // Số điện thoại đã có tài khoản rồi thì không cho đăng ký lại
        if (nguoiDungRepository.existsBySoDienThoai(phone)) {
            throw new BusinessException("Số điện thoại này đã được đăng ký tài khoản!");
        }

        String cooldownKey = OTP_REGISTER_PREFIX + phone + ":cooldown";
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new BusinessException("Bạn vừa yêu cầu mã OTP, vui lòng đợi ít phút rồi thử lại!");
        }

        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        String otpKey = OTP_REGISTER_PREFIX + phone;

        redisTemplate.opsForValue().set(otpKey, otp, OTP_TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(cooldownKey, "1", OTP_RESEND_COOLDOWN_SECONDS, TimeUnit.SECONDS);

        smsService.sendOtp(phone, otp);
    }

    // ═══════════════════════════════════════════════════════════════════
    // T065 - Bước 2: Xác thực OTP + tạo tài khoản, tự động đăng nhập luôn
    // ═══════════════════════════════════════════════════════════════════
    @Transactional
    public AuthResponse verifyRegisterOtp(VerifyPhoneOtpRequest request) {
        String phone = request.getSoDienThoai();
        String otpKey = OTP_REGISTER_PREFIX + phone;

        String savedOtp = redisTemplate.opsForValue().get(otpKey);
        if (savedOtp == null) {
            throw new BusinessException("Mã OTP đã hết hạn hoặc chưa được gửi, vui lòng yêu cầu mã mới!");
        }
        if (!savedOtp.equals(request.getOtp())) {
            throw new BusinessException("Mã OTP không chính xác!");
        }

        // Dùng 1 lần xong xóa ngay, tránh bị lợi dụng gọi lại API nhiều lần
        redisTemplate.delete(otpKey);

        // Kiểm tra lại lần cuối (chống trường hợp 2 request chạy song song
        // cùng đăng ký 1 số điện thoại trong lúc chờ xác thực OTP)
        if (nguoiDungRepository.existsBySoDienThoai(phone)) {
            throw new BusinessException("Số điện thoại này đã được đăng ký tài khoản!");
        }

        String hoTen = (request.getHoTen() == null || request.getHoTen().isBlank())
                ? "Khách hàng"
                : request.getHoTen();

        // Email nội bộ (không hiển thị cho người dùng) để thỏa ràng buộc
        // NOT NULL UNIQUE của cột email — tài khoản này chỉ đăng nhập lại
        // qua OTP SĐT, không dùng luồng đăng nhập email/mật khẩu.
        String internalEmail = phone + PHONE_EMAIL_SUFFIX;

        String rawPassword = (request.getMatKhau() == null || request.getMatKhau().isBlank())
                ? UUID.randomUUID().toString()
                : request.getMatKhau();

        NguoiDung user = NguoiDung.builder()
                .hoTen(hoTen)
                .email(internalEmail)
                .matKhau(passwordEncoder.encode(rawPassword))
                .soDienThoai(phone)
                .quyen("KHACH_HANG")
                .trangThai("HOAT_DONG")
                .build();

        nguoiDungRepository.save(user);

        return issueTokensAndLog(user, "DANG_KY_OTP_SDT");
    }

    // ═══════════════════════════════════════════════════════════════════
    // T065 - Đăng nhập / Đăng ký bằng Google OAuth2
    // Lần đầu (chưa có tài khoản khớp email/googleId) -> tự tạo tài khoản
    // Các lần sau -> tìm thấy tài khoản cũ -> đăng nhập thẳng luôn
    // ═══════════════════════════════════════════════════════════════════
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        Map<String, Object> payload = verifyGoogleIdToken(request.getIdToken());

        String googleId = (String) payload.get("sub");
        String email = (String) payload.get("email");
        String emailVerified = String.valueOf(payload.get("email_verified"));
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        if (googleId == null || email == null) {
            throw new BusinessException("Không lấy được thông tin tài khoản Google!");
        }
        if (!"true".equalsIgnoreCase(emailVerified)) {
            throw new BusinessException("Email Google chưa được xác thực!");
        }

        // 1. Ưu tiên tìm theo googleId (định danh không đổi, kể cả khi đổi email)
        Optional<NguoiDung> userOpt = nguoiDungRepository.findByGoogleId(googleId);

        // 2. Chưa liên kết googleId -> thử tìm theo email (VD: user từng đăng
        //    ký thường bằng đúng email đó) rồi liên kết googleId vào luôn
        if (userOpt.isEmpty()) {
            userOpt = nguoiDungRepository.findByEmail(email);
        }

        NguoiDung user;
        String hanhDong;

        if (userOpt.isPresent()) {
            // ĐÃ CÓ TÀI KHOẢN -> chỉ đăng nhập, không tạo mới
            user = userOpt.get();
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
            }
            if (user.getAnhDaiDien() == null && picture != null) {
                user.setAnhDaiDien(picture);
            }
            nguoiDungRepository.save(user);
            hanhDong = "DANG_NHAP_GOOGLE";
        } else {
            // CHƯA CÓ TÀI KHOẢN -> lần đầu bấm "Đăng nhập Google" sẽ tự tạo mới
            user = NguoiDung.builder()
                    .hoTen((name == null || name.isBlank()) ? "Khách hàng" : name)
                    .email(email)
                    .matKhau(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .anhDaiDien(picture)
                    .googleId(googleId)
                    .quyen("KHACH_HANG")
                    .trangThai("HOAT_DONG")
                    .build();
            nguoiDungRepository.save(user);
            hanhDong = "DANG_KY_GOOGLE";
        }

        // Áp dụng chung điều kiện trạng thái tài khoản như đăng nhập thường
        if ("BI_KHOA".equals(user.getTrangThai())) {
            throw new BusinessException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên để được hỗ trợ!");
        }
        if ("NGUNG_HOAT_DONG".equals(user.getTrangThai())) {
            throw new BusinessException("Tài khoản của bạn đã ngừng hoạt động!");
        }

        return issueTokensAndLog(user, hanhDong);
    }

    // Gọi endpoint tokeninfo chính thức của Google để xác thực chữ ký +
    // hạn dùng của idToken, đồng thời kiểm tra "aud" (Client ID) để chống
    // trường hợp kẻ tấn công gửi lên 1 idToken hợp lệ nhưng được cấp cho
    // một ứng dụng Google khác (audience confusion / token substitution).
    private Map<String, Object> verifyGoogleIdToken(String idToken) {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        Map<String, Object> payload;
        try {
            payload = restTemplate.getForObject(url, Map.class);
        } catch (HttpClientErrorException e) {
            throw new BusinessException("Google idToken không hợp lệ hoặc đã hết hạn!");
        } catch (Exception e) {
            log.error("Lỗi khi xác thực Google idToken: {}", e.getMessage());
            throw new BusinessException("Không thể xác thực tài khoản Google, vui lòng thử lại!");
        }

        if (payload == null) {
            throw new BusinessException("Google idToken không hợp lệ!");
        }

        Object aud = payload.get("aud");
        if (aud == null || !googleClientId.equals(aud.toString())) {
            throw new BusinessException("idToken không được cấp cho ứng dụng này!");
        }

        return payload;
    }

    // Gom logic tạo JWT + lưu refresh token + ghi nhật ký, dùng chung cho
    // login thường, đăng ký OTP SĐT và đăng nhập Google
    private AuthResponse issueTokensAndLog(NguoiDung user, String hanhDong) {
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getQuyen());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        lamMoiTokenRepository.deleteByNguoiDung(user);
        LamMoiToken rtEntity = LamMoiToken.builder()
                .nguoiDung(user)
                .token(refreshToken)
                .ngayHetHan(LocalDateTime.now().plusDays(7))
                .build();
        lamMoiTokenRepository.save(rtEntity);

        nhatKyHeThongRepository.save(NhatKyHeThong.builder()
                .nguoiDung(user)
                .hanhDong(hanhDong)
                .build());

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        return response;
    }

    // ═══════════════════════════════════════════════════════════════════
    // T091: Thiết lập và xác minh 2FA TOTP
    // ═══════════════════════════════════════════════════════════════════
    @Transactional
    public TotpSetupResponse setupTotp(String email) {
        NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại!"));

        if (Boolean.TRUE.equals(user.getIs2FaEnabled())) {
            throw new BusinessException("Tài khoản đã được bật 2FA từ trước!");
        }

        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        nguoiDungRepository.save(user);

        String qrCodeUri = totpService.getQrCodeUri(secret, email);

        TotpSetupResponse response = new TotpSetupResponse();
        response.setSecret(secret);
        response.setQrCodeUri(qrCodeUri);
        return response;
    }

    @Transactional
    public void verifyAndEnableTotp(String email, String code) {
        NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại!"));

        if (Boolean.TRUE.equals(user.getIs2FaEnabled())) {
            throw new BusinessException("Tài khoản đã được bật 2FA từ trước!");
        }

        if (user.getTotpSecret() == null) {
            throw new BusinessException("Vui lòng thiết lập 2FA trước khi xác minh!");
        }

        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            throw new BusinessException("Mã xác thực không chính xác!");
        }

        user.setIs2FaEnabled(true);
        nguoiDungRepository.save(user);
    }

    @Transactional
    public void disableTotp(String email) {
        NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại!"));

        if (!Boolean.TRUE.equals(user.getIs2FaEnabled())) {
            throw new BusinessException("Tài khoản chưa bật 2FA!");
        }

        user.setIs2FaEnabled(false);
        user.setTotpSecret(null); // Optional: clear secret
        nguoiDungRepository.save(user);
    }
}