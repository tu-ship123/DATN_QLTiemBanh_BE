package com.poly.cake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Bắt buộc để @Scheduled trong RateLimitingFilter.cleanupOldEntries() chạy được
@EnableAsync      // T103: Bật để TelegramWebhookService dùng @Async gửi thông báo không blocking
public class TiemBanhApplication {

    public static void main(String[] args) {
        SpringApplication.run(TiemBanhApplication.class, args);
    }

}

