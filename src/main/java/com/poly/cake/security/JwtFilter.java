package com.poly.cake.security;

import com.poly.cake.service.RedisTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final RedisTokenService redisTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // [SỬA LỖI] Bỏ qua kiểm tra Token nếu là API Auth
        String path = request.getServletPath();
        if (path.startsWith("/api/v1/auth/") && !path.equals("/api/v1/auth/logout")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String email;

        // Kiểm tra Header có chứa Bearer Token không
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        // Kiểm tra xem Token có bị Blacklist không (Đã đăng xuất)
        if (redisTokenService.isTokenBlacklisted(jwt)) {
            sendUnauthorized(response, "Token has been blacklisted. Please login again.");
            return;
        }

        try {
            // Validate Token và thiết lập SecurityContext
            if (jwtUtil.isTokenValid(jwt)) {
                email = jwtUtil.extractEmail(jwt);
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } else {
                // Token sai chữ ký hoặc đã hết hạn -> trả 401 rõ ràng ngay tại đây,
                // không để request đi tiếp như một user ẩn danh (tránh FE nhận nhầm 403).
                log.debug("Token không hợp lệ hoặc đã hết hạn cho request: {}", path);
                sendUnauthorized(response, "Token không hợp lệ hoặc đã hết hạn, vui lòng đăng nhập lại!");
                return;
            }
        } catch (Exception e) {
            log.debug("Lỗi giải mã JWT Token: {}", e.getMessage());
            sendUnauthorized(response, "Token không hợp lệ, vui lòng đăng nhập lại!");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"" + message + "\"}");
    }
}
