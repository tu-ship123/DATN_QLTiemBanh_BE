package com.poly.cake.config;

import com.poly.cake.security.JwtFilter;
import com.poly.cake.security.CustomAccessDeniedHandler;
import com.poly.cake.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // <-- Thêm thư viện này
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource; // <-- Sửa kiểu dữ liệu trả về
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays; // <-- Thêm thư viện này

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final AuthenticationProvider authenticationProvider;
    private final RateLimitingFilter rateLimitingFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // 1. Kích hoạt CORS chuẩn xác
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) 

                // CẤU HÌNH PHÂN QUYỀN API
                .authorizeHttpRequests(auth -> auth
                        // 2. [QUAN TRỌNG] Cho phép tất cả các request thăm dò (OPTIONS) được đi qua mượt mà
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers("/api/v1/payment/sepay-webhook").permitAll()
                        .requestMatchers("/api/v1/auth/**", "/api/v1/products/**", "/api/v1/categories/**", "/ws-bakery/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/pos/**", "/api/v1/shifts/**").hasAnyRole("ADMIN", "NHAN_VIEN")
                        .requestMatchers("/api/v1/cart/**", "/api/v1/orders/**").hasAnyRole("KHACH_HANG", "ADMIN", "NHAN_VIEN")
                        .anyRequest().authenticated()
                )

                // XỬ LÝ NGOẠI LỆ
                .exceptionHandling(customizer ->
                        customizer.accessDeniedHandler(new CustomAccessDeniedHandler())
                )

                // CẤU HÌNH FILTER
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)

                // Gắn "Bảo vệ cổng" chống Spam lên trước tiên, sau đó mới đến kiểm tra Token
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 3. Cấu hình chi tiết CORS cho Frontend Vue.js (Bao quát 100%)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        
        // Dùng OriginPatterns để hốt trọn cả localhost lẫn 127.0.0.1 ở mọi cổng
        config.setAllowedOriginPatterns(Arrays.asList("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}