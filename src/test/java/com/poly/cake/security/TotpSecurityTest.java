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
 * TT12 — TOTP / 2FA Security Tests
 * Kiểm tra các endpoint setup/verify/disable TOTP đều yêu cầu xác thực
 */
@SpringBootTest
@AutoConfigureMockMvc
public class TotpSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // ═══════════════════════════════════════════════════════════════
    // TOTP Setup Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    void testTotpSetup_WithoutToken_ShouldReturn401() throws Exception {
        // GET /api/v1/auth/totp/setup yêu cầu isAuthenticated() → 401 nếu không có token
        mockMvc.perform(get("/api/v1/auth/totp/setup"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testTotpSetup_WithFakeToken_ShouldReturn401() throws Exception {
        // Token giả bị JwtFilter từ chối → 401
        mockMvc.perform(get("/api/v1/auth/totp/setup")
                .header("Authorization", "Bearer fake.jwt.token.here"))
                .andExpect(status().isUnauthorized());
    }

    // ═══════════════════════════════════════════════════════════════
    // TOTP Verify Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    void testTotpVerify_WithoutToken_ShouldReturn401() throws Exception {
        // POST /api/v1/auth/totp/verify yêu cầu isAuthenticated() → 401
        mockMvc.perform(post("/api/v1/auth/totp/verify")
                .param("code", "123456"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testTotpVerify_WithFakeToken_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/totp/verify")
                .header("Authorization", "Bearer completely.invalid.token")
                .param("code", "000000"))
                .andExpect(status().isUnauthorized());
    }

    // ═══════════════════════════════════════════════════════════════
    // TOTP Disable Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    void testTotpDisable_WithoutToken_ShouldReturn401() throws Exception {
        // POST /api/v1/auth/totp/disable yêu cầu isAuthenticated() → 401
        mockMvc.perform(post("/api/v1/auth/totp/disable"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testTotpDisable_WithFakeToken_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/totp/disable")
                .header("Authorization", "Bearer bad.token.value"))
                .andExpect(status().isUnauthorized());
    }

    // ═══════════════════════════════════════════════════════════════
    // Logout Tests (cũng yêu cầu auth)
    // ═══════════════════════════════════════════════════════════════

    @Test
    void testLogout_WithoutToken_ShouldReturn401() throws Exception {
        // POST /api/v1/auth/logout có @PreAuthorize("isAuthenticated()") → 401
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    // ═══════════════════════════════════════════════════════════════
    // Public Auth Endpoints (KHÔNG cần token)
    // ═══════════════════════════════════════════════════════════════

    @Test
    void testLogin_PublicEndpoint_ShouldBeAccessible() throws Exception {
        // /auth/login là public → không bị chặn bởi SecurityConfig
        // Dù body sai → trả về 4xx nhưng KHÔNG phải 401 do thiếu token
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"notexist@test.com\",\"matKhau\":\"wrongpass\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Không phải lỗi do SecurityConfig block → không phải 403
                    // có thể là 400 (validation) hoặc 401 (sai credentials) từ AuthService
                    assert status != 403 : "Login endpoint bị block bởi SecurityConfig là sai!";
                });
    }

    @Test
    void testForgotPassword_PublicEndpoint_ShouldBeAccessible() throws Exception {
        // /auth/forgot-password là public
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 403 : "forgot-password không được block bởi SecurityConfig";
                    assert status != 401 : "forgot-password không cần token";
                });
    }
}
