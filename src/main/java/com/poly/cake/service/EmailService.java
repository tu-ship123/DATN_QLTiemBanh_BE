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

    // ✅ Gửi email khuyến mãi voucher đến khách hàng
    public void sendPromoVoucherEmail(String toEmail, String hoTen,
                                      String maCode, String loaiGiamGia,
                                      String giaTriGiam, String ngayHetHan,
                                      String donHangToiThieu) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("🎉 Ưu đãi đặc biệt dành riêng cho bạn - Chocopine");

        String loaiGiam = "PHAN_TRAM".equals(loaiGiamGia)
                ? "Giảm " + giaTriGiam + "% cho đơn hàng"
                : "Giảm " + giaTriGiam + "đ cho đơn hàng";

        String dieuKien = (donHangToiThieu != null && !donHangToiThieu.equals("0"))
                ? "\n   Đơn hàng tối thiểu: " + donHangToiThieu + "đ"
                : "";

        message.setText(
            "Xin chào " + hoTen + ",\n\n" +
            "🎂 Chocopine gửi đến bạn một ưu đãi đặc biệt!\n\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "   MÃ GIẢM GIÁ: " + maCode + "\n" +
            "   Nội dung: " + loaiGiam + dieuKien + "\n" +
            "   Hạn sử dụng: " + ngayHetHan + "\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "👉 Cách sử dụng:\n" +
            "   1. Truy cập website Chocopine\n" +
            "   2. Chọn sản phẩm yêu thích và thêm vào giỏ hàng\n" +
            "   3. Nhập mã \"" + maCode + "\" tại bước thanh toán\n" +
            "   4. Tận hưởng ưu đãi!\n\n" +
            "Đừng bỏ lỡ cơ hội này nhé! Mã chỉ có hiệu lực đến " + ngayHetHan + ".\n\n" +
            "Trân trọng,\n" +
            "Đội ngũ Chocopine 🍰"
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