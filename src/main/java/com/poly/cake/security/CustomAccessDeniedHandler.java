package com.poly.cake.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exc) throws IOException, ServletException {
        // Ép kiểu trả về là JSON và hỗ trợ tiếng Việt
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        // Chuỗi JSON báo lỗi chuẩn form
        String jsonFormat = "{\n" +
                "  \"status\": 403,\n" +
                "  \"error\": \"Forbidden\",\n" +
                "  \"message\": \"Bạn không đủ quyền hạn để thực hiện thao tác này!\"\n" +
                "}";

        response.getWriter().write(jsonFormat);
    }
}