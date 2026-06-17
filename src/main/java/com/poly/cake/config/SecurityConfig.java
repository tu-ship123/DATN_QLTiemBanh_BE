package com.poly.cake.config;


import com.poly.cake.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Tắt CSRF vì dùng JWT
            .cors(cors -> cors.configure(http)) // Bật CORS
            .authorizeHttpRequests(auth -> auth
                // Public APIs (Đăng nhập, đăng ký, xem sản phẩm...)
                .requestMatchers("/api/v1/auth/**", "/api/v1/products/**", "/api/v1/categories/**").permitAll()
                
                // Route dành riêng cho Admin
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                
                // Route dành cho Nhân viên hoặc Admin
                .requestMatchers("/api/v1/pos/**", "/api/v1/shifts/**").hasAnyRole("ADMIN", "NHAN_VIEN")
                
                // Route dành cho Khách hàng (Đã đăng nhập)
                .requestMatchers("/api/v1/cart/**", "/api/v1/orders/**").hasAnyRole("KHACH_HANG", "ADMIN", "NHAN_VIEN")
                
                // Các Request khác bắt buộc phải có Token
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}