package com.poly.cake.config;

import com.poly.cake.security.JwtFilter;
import com.poly.cake.security.CustomAccessDeniedHandler;
// Lớp RateLimitingFilter em vừa tạo cùng thư mục nên không cần import thêm
import com.poly.cake.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final AuthenticationProvider authenticationProvider;
    private final RateLimitingFilter rateLimitingFilter; // 1. Bơm bộ lọc chống Spam vào đây

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 2. Kích hoạt CORS chuẩn xác

                // CẤU HÌNH PHÂN QUYỀN API
                .authorizeHttpRequests(auth -> auth
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

                // 3. Gắn "Bảo vệ cổng" chống Spam lên trước tiên, sau đó mới đến kiểm tra Token
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 4. Cấu hình chi tiết CORS cho Frontend Vue.js
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:5173"); // Port mặc định của Vite (Vue 3)
        config.addAllowedOrigin("http://localhost:8080"); // Port của Vue CLI
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter filter) {
    FilterRegistrationBean<JwtFilter> registrationBean = new FilterRegistrationBean<>(filter);
    registrationBean.setEnabled(false); // Không cho Spring Boot tự đăng ký global
    return registrationBean;
}

@Bean
public FilterRegistrationBean<RateLimitingFilter> rateLimitFilterRegistration(RateLimitingFilter filter) {
    FilterRegistrationBean<RateLimitingFilter> registrationBean = new FilterRegistrationBean<>(filter);
    registrationBean.setEnabled(false);
    return registrationBean;
}
}