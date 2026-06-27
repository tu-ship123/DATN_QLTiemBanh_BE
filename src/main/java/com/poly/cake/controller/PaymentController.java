package com.poly.cake.controller;

import com.poly.cake.dto.SePayWebhookDto;
import com.poly.cake.entity.TrangThaiDonHang;
import com.poly.cake.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;

    @Value("${sepay.webhook-token}")
    private String sepayToken;

    // Định nghĩa sẵn Regex (Quét tìm chữ PC nối liền với 1 dãy số, ví dụ: PC1234, ck PC88)
    private static final Pattern PC_PATTERN = Pattern.compile("PC(\\d+)");

    @PostMapping("/sepay-webhook")
    public ResponseEntity<String> handleSePayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SePayWebhookDto request) {

        // 1. Kiểm tra Token
        if (authHeader == null || !authHeader.replace("Bearer ", "").trim().equals(sepayToken)) {
            log.warn("Cảnh báo: Có người gọi Webhook SePay nhưng sai Token!");
            return ResponseEntity.status(403).body("Sai mã xác thực Webhook!");
        }

        // 2. Chỉ xử lý khi có tiền chảy VÀO ("in")
        if ("in".equalsIgnoreCase(request.getTransferType())) {
            String content = request.getContent();

            if (content != null) {
                // 3. Dùng Regex để quét tự động
                Matcher matcher = PC_PATTERN.matcher(content.toUpperCase()); // toUpperCase để lỡ khách viết pc1234 vẫn nhận

                if (matcher.find()) {
                    try {
                        // Lấy ra đúng nhóm số đằng sau chữ PC
                        Long orderId = Long.parseLong(matcher.group(1));
                        log.info("Thành công: Nhận {} VNĐ cho đơn hàng HD-{}", request.getTransferAmount(), orderId);


                        orderService.updatePaymentStatus(orderId, TrangThaiDonHang.DA_THANH_TOAN);
                    } catch (Exception e) {
                        log.error("Lỗi khi cập nhật trạng thái đơn hàng: {}", e.getMessage());
                    }
                } else {
                    log.warn("Bỏ qua Webhook: Không tìm thấy cú pháp hợp lệ (VD: PC1234) trong tin nhắn: '{}'", content);
                }
            }
        }

        // SePay yêu cầu bắt buộc phải trả về chữ "success" và HTTP 200 OK
        return ResponseEntity.ok("success");
    }
}