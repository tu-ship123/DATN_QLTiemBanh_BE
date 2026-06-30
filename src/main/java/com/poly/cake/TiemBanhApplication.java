package com.poly.cake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Bắt buộc để @Scheduled trong RateLimitingFilter.cleanupOldEntries() chạy được
public class TiemBanhApplication {

    public static void main(String[] args) {
        SpringApplication.run(TiemBanhApplication.class, args);
    }

}
