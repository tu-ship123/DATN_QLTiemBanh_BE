package com.poly.cake.controller;

import com.poly.cake.dto.SePayWebhookDto;
import com.poly.cake.exception.BusinessException;
import com.poly.cake.exception.ResourceNotFoundException;
import com.poly.cake.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;

    // Lấy token từ file application-dev.yml
    @Value("${sepay.webhook-token}")
    private String sepayToken;

    // TỐI ƯU #8: Regex tìm chính xác chữ DH và các con số liền sau (an toàn tuyệt đối)
    private static final Pattern DH_PATTERN = Pattern.compile("DH(\\d+)");

    @PostMapping("/sepay-webhook")
    public ResponseEntity<String> handleSePayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SePayWebhookDto request) {

        // 1. Kiểm tra "Mật khẩu" (Token) xem có đúng là SePay gọi không
        if (authHeader == null || !authHeader.replace("Apikey ", "").trim().equals(sepayToken)) {
            log.warn("Cảnh báo: Có người gọi Webhook SePay nhưng sai Token!");
            return ResponseEntity.status(403).body("Sai mã xác thực Webhook!");
        }

        // 2. Chỉ xử lý khi có tiền chảy VÀO tài khoản ("in")
        if ("in".equalsIgnoreCase(request.getTransferType())) {
            String content = request.getContent();

            // 3. Bóc tách nội dung chuyển khoản bằng Regex
            if (content != null) {
                Matcher matcher = DH_PATTERN.matcher(content.toUpperCase());

                if (matcher.find()) {
                    Long orderId = Long.parseLong(matcher.group(1));

                    // TỐI ƯU #10: Chia tầng lỗi để phân biệt rõ nguyên nhân khi có sự cố
                    try {
                        log.info("Thành công: Nhận {} VNĐ cho đơn hàng DH{}", request.getTransferAmount(), orderId);

                        // 4. Cập nhật trạng thái đơn hàng và thanh toán (có đối chiếu số tiền)
                        BigDecimal soTienNhanDuoc = request.getTransferAmount() != null
                                ? BigDecimal.valueOf(request.getTransferAmount())
                                : BigDecimal.ZERO;
                        orderService.updatePaymentStatus(orderId, soTienNhanDuoc);
                        log.info("✅ Xử lý Webhook SePay THÀNH CÔNG cho đơn hàng: DH{}", orderId);

                    } catch (ResourceNotFoundException e) {
                        // LỖI NGHIỆP VỤ: Tìm thấy mã số nhưng DB không có đơn này
                        log.error("🚨 Lỗi Webhook SePay (Nghiệp vụ): Nhận được tiền nhưng KHÔNG TỒN TẠI đơn hàng DH{} trong hệ thống!", orderId);

                    } catch (BusinessException e) {
                        // LỖI NGHIỆP VỤ: Chuyển khoản đúng nội dung nhưng THIẾU TIỀN so với tổng đơn hàng
                        log.warn("⚠️ Webhook SePay (Thiếu tiền) cho đơn DH{}: {}", orderId, e.getMessage());

                    } catch (Exception e) {
                        // LỖI HỆ THỐNG: Lỗi mạng, database sập, v.v.
                        log.error("💥 Lỗi Webhook SePay (Hệ thống) khi xử lý đơn DH{}: {}", orderId, e.getMessage());
                    }

                } else {
                    // LỖI PARSE: Khách không ghi mã đơn hoặc ghi sai cú pháp
                    log.warn("⚠️ Lỗi Webhook SePay (Parse): Không tìm thấy mã đơn hợp lệ trong nội dung CK: [{}]", content);
                }
            }
        }

        // LUÔN LUÔN trả về 200 OK để SePay không gửi lại request này nữa (tránh nghẽn server)
        return ResponseEntity.ok("success");
    }
}