package com.poly.cake.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Dịch vụ gửi SMS OTP đăng ký bằng số điện thoại.
 *
 * LƯU Ý QUAN TRỌNG: Hiện tại project CHƯA tích hợp nhà cung cấp SMS thật
 * (VD: eSMS.vn, Speed SMS, Twilio, Firebase Phone Auth...) vì cần tài khoản
 * trả phí + đăng ký brandname. Để không chặn tiến độ, class này tạm thời
 * chỉ GHI LOG mã OTP ra console/log file (giống hệt cách nhiều dự án sinh
 * viên/demo vẫn làm ở giai đoạn dev).
 *
 * KHI TRIỂN KHAI THẬT: chỉ cần sửa duy nhất method sendOtp() bên dưới để
 * gọi API của nhà cung cấp SMS, phần còn lại của hệ thống (AuthService,
 * Controller...) KHÔNG cần thay đổi gì.
 */
@Slf4j
@Service
public class SmsService {

    public void sendOtp(String soDienThoai, String otp) {
        // TODO: Thay đoạn log này bằng lệnh gọi API nhà mạng/SMS Gateway thật
        // Ví dụ (eSMS.vn / Speed SMS / Twilio...):
        //   restTemplate.postForEntity(smsProviderUrl, requestBody, String.class);
        log.info("[SMS OTP] Gửi mã OTP '{}' đến số điện thoại {} (hiệu lực 5 phút)", otp, soDienThoai);
    }
}
