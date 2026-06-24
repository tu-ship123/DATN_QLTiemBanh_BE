package com.poly.cake.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // Bộ nhớ đệm lưu IP và thông tin request (Thread-safe)
    private final Map<String, RequestInfo> requestCounts = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS_PER_MINUTE = 100; // Giới hạn 100 req/phút
    private static final long ONE_MINUTE_IN_MILLIS = 60000; // 1 phút = 60.000 ms

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Lấy IP thật của người dùng (kể cả khi qua Proxy/Nginx)
        String clientIp = getClientIP(request);
        long currentTime = System.currentTimeMillis();

        requestCounts.putIfAbsent(clientIp, new RequestInfo(currentTime, 0));
        RequestInfo requestInfo = requestCounts.get(clientIp);

        // Khóa object để đảm bảo an toàn khi nhiều request gọi tới cùng lúc
        synchronized (requestInfo) {
            // Nếu đã qua 1 phút -> Reset lại bộ đếm
            if (currentTime - requestInfo.startTime > ONE_MINUTE_IN_MILLIS) {
                requestInfo.startTime = currentTime;
                requestInfo.count = 1;
            } else {
                requestInfo.count++;
                // Nếu spam vượt giới hạn -> Khóa tạm thời
                if (requestInfo.count > MAX_REQUESTS_PER_MINUTE) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // Lỗi 429
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"message\": \"Spam detected! IP của bạn đã bị khóa tạm thời. Vui lòng thử lại sau 1 phút.\"}");
                    return; // Chặn đứng tại đây, không cho đi tiếp vào Controller
                }
            }
        }

        // Cho phép đi tiếp
        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    // Lớp chứa thông tin bộ đếm
    private static class RequestInfo {
        long startTime;
        int count;

        RequestInfo(long startTime, int count) {
            this.startTime = startTime;
            this.count = count;
        }
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Cấp "thẻ miễn tử" (bỏ qua Rate Limit) cho các API công khai phục vụ hiển thị giao diện
        return path.startsWith("/api/v1/products")
                || path.startsWith("/api/v1/categories")
                || path.startsWith("/api/v1/auth")  
                || path.startsWith("/ws-bakery") // Bỏ qua luôn cho WebSocket (nếu có)
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}