package com.poly.cake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class TiemBanhApplication {

    public static void main(String[] args) {
        SpringApplication.run(TiemBanhApplication.class, args);
    }

    // TẠM THỜI thêm đoạn này để test hash, xóa sau khi xong
    @Bean
    public CommandLineRunner testHash() {
        return args -> {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String newHash = encoder.encode("123456");
            System.out.println("===> HASH MOI CHO 123456: " + newHash);
        };
    }
}