package com.poly.cake.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;


    public void sendTempPasswordToStaff(String toEmail, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("🔒 [Poly Cake] Thông tin tài khoản nhân viên mới");
        message.setText("Chào bạn,\n\n" +
                "Tài khoản nhân viên của bạn đã được khởi tạo thành công trên hệ thống.\n" +
                "Mật khẩu đăng nhập tạm thời của bạn là: " + tempPassword + "\n\n" +
                "Vui lòng đăng nhập và đổi mật khẩu ngay để đảm bảo an toàn.\n\n" +
                "Trân trọng,\nBan Quản Trị Tiệm Bánh");

        mailSender.send(message);
    }
}