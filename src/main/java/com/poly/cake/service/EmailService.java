package com.poly.cake.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

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