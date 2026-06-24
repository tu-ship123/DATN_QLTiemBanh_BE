package com.poly.cake.controller;

import com.poly.cake.dto.SePayWebhookDto;
import com.poly.cake.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;

    // Lấy token từ file application-dev.yml
    @Value("${sepay.webhook-token}")
    private String sepayToken;

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

            // 3. Bóc tách nội dung chuyển khoản tìm chữ "DH" (DonHang)
            if (content != null && content.contains("DH")) {
                try {
                    // Lấy con số đằng sau chữ DH (Ví dụ: "DH1234" -> "1234")
                    String orderIdStr = content.substring(content.indexOf("DH") + 2).trim().split(" ")[0];
                    Long orderId = Long.parseLong(orderIdStr);

                    log.info("Thành công: Nhận {} VNĐ cho đơn hàng DH{}", request.getTransferAmount(), orderId);

                    // 4. Cập nhật trạng thái đơn hàng và thanh toán
                    orderService.updatePaymentStatus(orderId);

                } catch (Exception e) {
                    log.error("Không thể lấy ID đơn hàng từ nội dung: {}", content);
                }
            }
        }

        // SePay yêu cầu bắt buộc phải trả về chữ "success" và HTTP 200 OK
        return ResponseEntity.ok("success");
    }
}