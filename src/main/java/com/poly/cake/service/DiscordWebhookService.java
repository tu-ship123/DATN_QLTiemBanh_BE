package com.poly.cake.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * T103 – Discord Webhook Service
 * Gửi thông báo tự động đến kênh Discord khi có sự kiện quan trọng.
 * Dùng Discord Webhook tiện hơn Telegram vì chỉ cần 1 URL duy nhất.
 *
 * Config trong application.yml:
 *   discord:
 *     webhook-url: "https://discord.com/api/webhooks/..."
 *     enabled: true
 */
@Slf4j
@Service
public class DiscordWebhookService {

    @Value("${discord.webhook-url:}")
    private String webhookUrl;

    @Value("${discord.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public void sendNewOrderNotification(Long orderId, String customerName, double totalAmount) {
        String message = String.format(
                "🛒 **ĐƠN HÀNG MỚI** 🛒\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "📦 **Mã đơn:** HD-%d\n" +
                "👤 **Khách hàng:** %s\n" +
                "💰 **Tổng tiền:** %,.0f đ\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "👉 *Vào admin để xác nhận đơn hàng ngay!*",
                orderId, customerName, totalAmount
        );
        sendMessage(message);
    }

    @Async
    public void sendOrderCancelledNotification(Long orderId, String customerName, String reason, boolean hasRefund) {
        String message = String.format(
                "❌ **ĐƠN HÀNG BỊ HỦY** ❌\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "📦 **Mã đơn:** HD-%d\n" +
                "👤 **Khách hàng:** %s\n" +
                "📝 **Lý do:** %s\n" +
                "%s" +
                "━━━━━━━━━━━━━━━━━━━━",
                orderId, customerName, reason,
                hasRefund ? "💸 **CẦN XỬ LÝ HOÀN TIỀN!**\n" : ""
        );
        sendMessage(message);
    }

    @Async
    public void sendLowStockAlert(String productName, int currentStock, int threshold) {
        String message = String.format(
                "⚠️ **CẢNH BÁO TỒN KHO THẤP** ⚠️\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "🎂 **Sản phẩm:** %s\n" +
                "📊 **Tồn kho hiện tại:** %d\n" +
                "🔔 **Ngưỡng cảnh báo:** %d\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "👉 *Vui lòng nhập hàng sớm!*",
                productName, currentStock, threshold
        );
        sendMessage(message);
    }

    public void sendMessage(String text) {
        if (!enabled) {
            log.debug("[Discord] Disabled. Message skipped: {}", text.substring(0, Math.min(50, text.length())));
            return;
        }
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[Discord] webhook-url chưa được cấu hình. Message skipped.");
            return;
        }

        try {
            // Discord API nhận payload có field "content"
            Map<String, Object> body = Map.of("content", text);
            restTemplate.postForObject(webhookUrl, body, String.class);
            log.info("[Discord] Message sent successfully to webhook.");
        } catch (Exception e) {
            log.error("[Discord] Gửi thông báo thất bại: {}", e.getMessage());
        }
    }

    // ── Dùng cho trang Admin > Webhook ──────────────────────────────────────
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConfigured() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }

    // Che bớt URL thật, chỉ hiện phần đầu/cuối để Admin xác nhận đã cấu hình đúng kênh
    // mà không lộ toàn bộ webhook URL (ai có URL này đều gửi tin nhắn được vào kênh Discord).
    public String getMaskedUrl() {
        if (!isConfigured()) return null;
        if (webhookUrl.length() <= 20) return "****";
        return webhookUrl.substring(0, 40) + "..." + webhookUrl.substring(webhookUrl.length() - 6);
    }

    // Gửi tin nhắn test, trả về true/false để FE báo kết quả ngay (thay vì chỉ ghi log)
    public boolean sendTestMessage() {
        if (!enabled || !isConfigured()) return false;
        try {
            Map<String, Object> body = Map.of("content", "🔔 Đây là tin nhắn test từ trang Quản trị Chocopine.");
            restTemplate.postForObject(webhookUrl, body, String.class);
            return true;
        } catch (Exception e) {
            log.error("[Discord] Gửi test message thất bại: {}", e.getMessage());
            return false;
        }
    }
}
