package com.poly.cake.config;

import com.poly.cake.security.JwtFilter;
import com.poly.cake.security.CustomAccessDeniedHandler; // Import class xử lý lỗi 403
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // Import thiếu của em
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Kích hoạt tính năng @PreAuthorize tại các Controller
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Tắt CSRF vì dùng JWT
                .cors(cors -> cors.configure(http)) // Bật CORS

                // 1. CẤU HÌNH PHÂN QUYỀN API
                .authorizeHttpRequests(auth -> auth
                        // Public APIs (Đi qua không cần token)
                        .requestMatchers("/api/v1/auth/**", "/api/v1/products/**", "/api/v1/categories/**", "/ws-bakery/**").permitAll()

                        // Route dành riêng cho Admin
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Route dành cho Nhân viên hoặc Admin
                        .requestMatchers("/api/v1/pos/**", "/api/v1/shifts/**").hasAnyRole("ADMIN", "NHAN_VIEN")

                        // Route dành cho Khách hàng (Đã đăng nhập)
                        .requestMatchers("/api/v1/cart/**", "/api/v1/orders/**").hasAnyRole("KHACH_HANG", "ADMIN", "NHAN_VIEN")

                        // Các Request khác bắt buộc phải có Token hợp lệ
                        .anyRequest().authenticated()
                )

                // 2. XỬ LÝ NGOẠI LỆ (TRẢ VỀ JSON CHO VUE.JS)
                .exceptionHandling(customizer ->
                        customizer.accessDeniedHandler(new CustomAccessDeniedHandler())
                )

                // 3. CẤU HÌNH JWT FILTER
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}