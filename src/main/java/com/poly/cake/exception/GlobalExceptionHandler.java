package com.poly.cake.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@Slf4j
@RestControllerAdvice // Đánh dấu đây là "Phễu" hứng lỗi cho toàn bộ API
public class GlobalExceptionHandler {

    // 1. Chuyên bắt các lỗi không tìm thấy dữ liệu (như sai ID nhân viên, sai ID đơn hàng)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Không tìm thấy dữ liệu: {}", ex.getMessage());
        // Luôn trả về định dạng JSON { "error": "..." } để Vue.js dễ đọc
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    // 2. Chuyên bắt TẤT CẢ các lỗi còn lại (Trạm gác cuối cùng)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception ex) {
        // Ghi log chi tiết ra màn hình đen Console để em dễ fix bug
        log.error("Lỗi hệ thống nghiêm trọng: ", ex);

        // Trả về Frontend một thông báo dạng JSON
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}