package com.poly.cake.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCors_ValidOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void testCors_InvalidOrigin_ShouldNotHaveAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                .header("Origin", "http://malicious-site.com")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden()); // SecurityConfig only allows specific origins, others may be forbidden or not have the header
    }

    @Test
    void testRateLimiting_TooManyRequests() throws Exception {
        // Gửi liên tục 15 requests trong 1 giây đến endpoint public
        for (int i = 0; i < 11; i++) {
            mockMvc.perform(get("/api/v1/products"))
                    .andReturn();
        }
        
        // Theo config mặc định, nếu vượt quá số lượng, sẽ bị trả về 429 Too Many Requests
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void testAuthentication_UnauthorizedAccess() throws Exception {
        // Cố gắng truy cập admin endpoint mà không có token
        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isUnauthorized()); // Do JwtFilter trả về 401 khi không có token
    }
}
