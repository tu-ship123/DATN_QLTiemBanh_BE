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
import org.springframework.dao.DataIntegrityViolationException;
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
    private final RestTemplate restTemplate;

    @Value("${google.client-id}")
    private String googleClientId;

    // [SỬA] Dùng SecureRandom thay cho Random để tạo OTP an toàn hơn
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // T007: Đăng ký
    @Transactional
    public void register(RegisterRequest request) {
        // Chuẩn hoá email: trim + lowercase, để "Test@Gmail.com" và "test@gmail.com"
        // được coi là CÙNG một email khi check trùng lẫn khi lưu (tránh lệch giữa
        // check và insert, đặc biệt khi DB collation không phân biệt hoa/thường).
        String email = request.getEmail().trim().toLowerCase();
        String soDienThoai = request.getSoDienThoai() != null ? request.getSoDienThoai().trim() : null;

        // Điều kiện 1: Email đã tồn tại -> không cho đăng ký trùng
        if (nguoiDungRepository.existsByEmail(email)) {
            throw new BusinessException("Email đã được sử dụng!");
        }

        // Điều kiện 2: Số điện thoại đã được dùng cho tài khoản khác -> báo lỗi rõ ràng cho FE
        if (soDienThoai != null && !soDienThoai.isBlank()
                && nguoiDungRepository.existsBySoDienThoai(soDienThoai)) {
            throw new BusinessException("Số điện thoại đã được sử dụng!");
        }

        NguoiDung user = NguoiDung.builder()
                .hoTen(request.getHoTen())
                .email(email)
                .matKhau(passwordEncoder.encode(request.getMatKhau()))
                .soDienThoai(soDienThoai)
                .quyen("KHACH_HANG")
                .trangThai("HOAT_DONG")
                .build();

        // Dùng try-catch thay vì chỉ dựa vào check ở trên: 2 check phía trên và
        // câu INSERT không nằm trong cùng 1 thao tác nguyên tử (atomic), nên nếu
        // 2 request đăng ký cùng dữ liệu được gửi gần như đồng thời (double-click,
        // mạng chậm khiến người dùng bấm lại...), CẢ HAI đều có thể "lọt" qua check
        // ở trên (vì lúc check, request kia chưa commit xong), request insert sau
        // sẽ vi phạm UNIQUE constraint ở DB.
        //
        // QUAN TRỌNG: sau khi save() ném DataIntegrityViolationException, Hibernate
        // Session coi như đã "nhiễm độc" (bị Hibernate cảnh báo:
        // "don't flush the Session after an exception occurs") — TUYỆT ĐỐI không
        // được gọi thêm bất kỳ repository/query nào khác trong cùng transaction
        // này nữa (kể cả existsByEmail/existsBySoDienThoai), nếu không sẽ ăn thêm
        // lỗi AssertionFailure ("null id in entry") đè lên lỗi gốc. Vì vậy ở đây
        // CHỈ đọc message của chính exception (tên constraint UNIQUE bị vi phạm,
        // xem db/003_..., db/006_..., db/007_...) để xác định trùng field nào,
        // không truy vấn lại DB.
        try {
            nguoiDungRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            String detail = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
            if (detail != null && detail.contains("UQ_nguoi_dung_so_dien_thoai")) {
                throw new BusinessException("Số điện thoại đã được sử dụng!");
            }
            if (detail != null && detail.contains("UQ_nguoi_dung_email")) {
                throw new BusinessException("Email đã được sử dụng!");
            }
            throw e; // Vi phạm ràng buộc khác -> để GlobalExceptionHandler xử lý (fallback 409 chung)
        }
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