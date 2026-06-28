package com.poly.cake.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // ✅ Gửi OTP quên mật khẩu
    public void sendPasswordResetOtp(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã xác nhận đặt lại mật khẩu - Chocopine");
        message.setText(
            "Xin chào,\n\n" +
            "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.\n\n" +
            "Mã OTP của bạn là: " + otp + "\n\n" +
            "Mã này có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.\n\n" +
            "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.\n\n" +
            "Trân trọng,\nĐội ngũ Chocopine"
        );
        mailSender.send(message);
    }

    public void sendNewStaffEmail(String toEmail, String fullName, String rawPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Thông tin tài khoản nhân viên PolyCake");

        // Nội dung bức thư
        message.setText("Chào " + fullName + ",\n\n" +
                "Tài khoản nhân viên của bạn đã được tạo thành công trên hệ thống PolyCake.\n\n" +
                "Thông tin đăng nhập:\n" +
                "- Tên đăng nhập: " + toEmail + "\n" +
                "- Mật khẩu tạm thời: " + rawPassword + "\n\n" +
                "Vì lý do bảo mật, vui lòng đăng nhập và đổi mật khẩu ngay trong lần đầu tiên sử dụng hệ thống.\n\n" +
                "Trân trọng,\nBan quản trị PolyCake");

        mailSender.send(message);
    }
}