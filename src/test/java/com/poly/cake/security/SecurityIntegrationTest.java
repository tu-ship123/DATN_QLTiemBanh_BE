package com.poly.cake.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TT12 — Security Integration Tests
 * Kiểm tra toàn diện: CORS, Rate Limiting, Authentication, RBAC
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ═══════════════════════════════════════════════════════════════
    // CORS Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    void testCors_ValidOrigin_ShouldAllowAccess() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void testCors_InvalidOrigin_ShouldNotHaveAllowOriginHeader() throws Exception {
        // SecurityConfig chỉ cho phép localhost:* và chocopine.xyz
        // malicious-site.com không nằm trong whitelist → không nhận được header
        mockMvc.perform(options("/api/v1/products")
                .header("Origin", "http://malicious-site.com")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCors_OptionsPreflightReturnsAllowMethodsHeader() throws Exception {
        // Kiểm tra header Access-Control-Allow-Methods có đầy đủ HTTP methods không
        mockMvc.perform(options("/api/v1/products")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void testCors_ProductionOrigin_ShouldAllowAccess() throws Exception {
        // Production origin (chocopine.xyz) phải được phép
        mockMvc.perform(options("/api/v1/products")
                .header("Origin", "https://app.chocopine.xyz")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    // ═══════════════════════════════════════════════════════════════
    // Rate Limiting Tests
    // [T105/TT12 BUG FIX] Trước đây test sai endpoint /api/v1/products
    // vì endpoint đó được whitelist trong shouldNotFilter() của RateLimitingFilter.
    // Sửa sang /api/v1/admin/orders - endpoint BỊ rate-limit (phải qua filter).
    // ═══════════════════════════════════════════════════════════════

    @Test
    void testRateLimiting_TooManyRequests_ShouldReturn429() throws Exception {
        // Endpoint /api/v1/admin/orders KHÔNG nằm trong shouldNotFilter() whitelist
        // → phải đi qua RateLimitingFilter. Giới hạn mặc định: 100 req/phút.
        // Gửi 101 requests từ cùng 1 IP giả lập → request thứ 101 phải nhận 429.
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/api/v1/admin/orders")).andReturn();
        }
        // Request vượt ngưỡng → bị chặn
        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void testRateLimiting_WhitelistedEndpoint_ShouldNotBeBlocked() throws Exception {
        // /api/v1/products nằm trong shouldNotFilter() → không bao giờ bị rate-limit
        // Gửi nhiều hơn ngưỡng nhưng không bị chặn
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Authentication Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    void testAuthentication_UnauthorizedAccess_ShouldReturn401() throws Exception {
        // Truy cập admin endpoint không có token → 401
        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAuthentication_WithInvalidBearerToken_ShouldReturn401() throws Exception {
        // Gửi token giả → JwtFilter từ chối → 401
        mockMvc.perform(get("/api/v1/admin/orders")
                .header("Authorization", "Bearer this.is.a.fake.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAuthentication_WithMalformedAuthHeader_ShouldReturn401() throws Exception {
        // Header Authorization không theo chuẩn "Bearer <token>" → 401
        mockMvc.perform(get("/api/v1/admin/orders")
                .header("Authorization", "InvalidFormat tokenhere"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testPublicEndpoint_ShouldBeAccessibleWithoutToken() throws Exception {
        // /api/v1/products là public endpoint → không cần token
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());
    }

    // ═══════════════════════════════════════════════════════════════
    // RBAC Tests (Role-Based Access Control)
    // ═══════════════════════════════════════════════════════════════

    @Test
    void testRbac_AuthEndpoints_ShouldBePublic() throws Exception {
        // Các auth endpoint phải public hoàn toàn, không cần token
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\",\"matKhau\":\"wrong\"}"))
                // Có thể nhận 400 (validation fail) hoặc 401 (sai mật khẩu) 
                // nhưng KHÔNG phải do thiếu auth token → không nhận 403 Forbidden do SecurityConfig
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 403 : "Auth endpoint không được trả về 403";
                });
    }
}
