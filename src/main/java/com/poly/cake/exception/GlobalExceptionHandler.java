package com.poly.cake.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Lỗi 404: Tìm không thấy
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    // Lỗi 400: Sai logic nghiệp vụ
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusiness(BusinessException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }

    // Lỗi 403: Không đủ quyền (do mình tự throw trong Service)
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<?> handleForbidden(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
    }

    // Lỗi 403: Spring Security tự ném khi @PreAuthorize chặn (không đủ role)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Bạn không có quyền thực hiện hành động này!"));
    }

    // Lỗi 400: @Valid trên @RequestBody fail (DanhGiaDto, OrderDto.Request, ...)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Dữ liệu không hợp lệ!", "errors", errors));
    }

    // Lỗi 409: Vi phạm ràng buộc UNIQUE ở DB (trùng email, trùng SĐT...)
    // Đây là lưới an toàn cuối cùng — nếu tầng Service lỡ quên check trùng trước khi save,
    // lỗi này vẫn được bắt lại và trả về thông báo thân thiện cho FE, thay vì lỗi 500 khó hiểu.
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException e) {
        log.warn("Vi phạm ràng buộc dữ liệu: ", e);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Dữ liệu bị trùng (email hoặc số điện thoại đã tồn tại)!"));
    }

    // Lỗi 400: Sai kiểu dữ liệu tham số (ví dụ @RequestParam Long id mà truyền "abc")
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Tham số không hợp lệ: " + e.getMessage()));
    }

    // Lỗi 400: Tham số trên URL (PathVariable/RequestParam) sai kiểu dữ liệu.
    // Ví dụ điển hình gây crash ở phần nhập kho: FE gọi PUT /phieu-nhap/{id}/approve
    // nhưng biến id ở FE đang là undefined -> BE nhận chuỗi "undefined" thay vì số ->
    // parse Long thất bại. Trước đây lỗi này rơi vào handler Exception chung ở dưới,
    // trả về 500 "lỗi hệ thống" khiến FE/người dùng khó hiểu vì tưởng lỗi server.
    // Thực chất đây là lỗi do tham số đầu vào không hợp lệ (400), không phải lỗi hệ thống.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String gioiThieuTen = e.getName();
        String giaTriNhan = String.valueOf(e.getValue());
        log.warn("Tham số '{}' không hợp lệ, giá trị nhận được: '{}'", gioiThieuTen, giaTriNhan);
        String message = "undefined".equals(giaTriNhan) || giaTriNhan == null
                ? "Thiếu thông tin '" + gioiThieuTen + "', vui lòng tải lại trang và thử lại!"
                : "Tham số '" + gioiThieuTen + "' không hợp lệ: '" + giaTriNhan + "'";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    // Trạm cuối: bắt mọi lỗi chưa lường trước (NPE, lỗi DB, lỗi parse...) -> LUÔN trả 500
    // Không bao giờ để lộ stack trace / e.getMessage() thô ra ngoài cho người dùng cuối.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception e) {
        log.error("Lỗi hệ thống không mong đợi: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Đã có lỗi xảy ra ở hệ thống, vui lòng thử lại sau!"));
    }
}