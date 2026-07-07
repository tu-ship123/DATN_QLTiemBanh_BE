package com.poly.cake.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    private static final DecimalFormat TIEN_FORMAT = new DecimalFormat("#,##0");

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

    // ═══════════════════════════════════════════════════════════════════
    // T072 – Gửi email xác nhận đặt hàng THÀNH CÔNG, đính kèm PDF hóa đơn.
    // Dùng MimeMessage (thay vì SimpleMailMessage) vì cần đính kèm file PDF.
    // ═══════════════════════════════════════════════════════════════════
    public void sendOrderConfirmationEmail(String toEmail, String hoTen, String maDonHang,
                                            byte[] pdfHoaDon) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            // "true" = multipart (bắt buộc để có thể đính kèm file)
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Xác nhận đặt hàng thành công - " + maDonHang + " - Chocopine");

            String noiDung =
                    "Xin chào " + (hoTen != null ? hoTen : "quý khách") + ",\n\n" +
                    "Cảm ơn bạn đã đặt hàng tại Chocopine! 🎂\n\n" +
                    "Đơn hàng " + maDonHang + " của bạn đã được ghi nhận thành công và đang chờ " +
                    "cửa hàng xác nhận. Chúng tôi đã đính kèm hóa đơn chi tiết (PDF) trong email này, " +
                    "bạn vui lòng lưu lại để đối chiếu khi cần.\n\n" +
                    "Bạn có thể theo dõi trạng thái đơn hàng bất kỳ lúc nào trong mục " +
                    "\"Đơn hàng của tôi\" trên website.\n\n" +
                    "Trân trọng,\nĐội ngũ Chocopine 🍰";
            helper.setText(noiDung, false);

            if (pdfHoaDon != null && pdfHoaDon.length > 0) {
                helper.addAttachment("HoaDon-" + maDonHang + ".pdf", new ByteArrayResource(pdfHoaDon));
            }

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            // Gửi email chỉ là hành động "best-effort" đi kèm, KHÔNG được phép làm
            // rollback giao dịch đặt hàng chính -> chỉ log lỗi, để caller tự quyết định.
            log.error("Gửi email xác nhận đơn hàng {} tới {} thất bại: {}", maDonHang, toEmail, e.getMessage(), e);
            throw new RuntimeException("Gửi email xác nhận đơn hàng thất bại: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // T072 – Gửi email thông báo khách đã hủy đơn thành công, kèm thông tin
    // hoàn tiền (nếu đơn đã thanh toán/đặt cọc trước đó).
    // ═══════════════════════════════════════════════════════════════════
    public void sendOrderCancellationEmail(String toEmail, String hoTen, String maDonHang,
                                            String lyDoHuy, BigDecimal soTienHoan) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Xác nhận hủy đơn hàng " + maDonHang + " - Chocopine");

        boolean coHoanTien = soTienHoan != null && soTienHoan.compareTo(BigDecimal.ZERO) > 0;
        String phanHoanTien = coHoanTien
                ? "\n💰 Số tiền hoàn lại: " + TIEN_FORMAT.format(soTienHoan) + "đ\n" +
                  "   Chúng tôi sẽ hoàn tiền về phương thức thanh toán ban đầu của bạn trong vòng " +
                  "1-3 ngày làm việc. Nếu quá thời gian trên mà chưa nhận được, vui lòng liên hệ " +
                  "hotline để được hỗ trợ.\n"
                : "\nĐơn hàng này chưa phát sinh thanh toán nên không có khoản tiền nào cần hoàn lại.\n";

        message.setText(
                "Xin chào " + (hoTen != null ? hoTen : "quý khách") + ",\n\n" +
                "Đơn hàng " + maDonHang + " của bạn đã được hủy thành công theo yêu cầu.\n\n" +
                "Lý do hủy: " + (lyDoHuy != null ? lyDoHuy : "Khách hàng tự hủy") + "\n" +
                phanHoanTien + "\n" +
                "Nếu đây không phải yêu cầu của bạn, vui lòng liên hệ Chocopine ngay để được hỗ trợ.\n\n" +
                "Trân trọng,\nĐội ngũ Chocopine 🍰"
        );
        mailSender.send(message);
    }
}