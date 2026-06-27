package com.poly.cake.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j // Dùng để ghi log lỗi ra Console
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. LỖI LOGIC NGHIỆP VỤ (Ví dụ: Hàng không đủ, Đơn đã hủy...) -> HTTP 400
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusiness(BusinessException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    // 2. LỖI KHÔNG TÌM THẤY TÀI NGUYÊN (Ví dụ: ID không tồn tại) -> HTTP 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    // 3. LỖI KHÔNG ĐỦ QUYỀN (IDOR, Cố tình xem đơn người khác) -> HTTP 403
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<?> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    // 4. LỖI VALIDATE DỮ LIỆU TỪ FRONTEND (@Valid) -> HTTP 400 kèm danh sách lỗi
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(errors);
    }

    // 5. TRẠM CUỐI: BẮT MỌI LỖI SERVER KHÔNG MONG ĐỢI (NPE, Đứt cáp DB...) -> HTTP 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception ex) {
        // Cực kỳ quan trọng: In lỗi thực sự ra màn hình Console để DEV còn biết đường sửa
        log.error("Lỗi server không mong đợi: ", ex);

        // Trả về cho Frontend một câu chung chung để giấu kỹ thuật bên trong
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Lỗi hệ thống, vui lòng thử lại sau."));
    }
}