package com.poly.cake.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotTomTatHoiThoaiDto {
    private String sessionId;
    private String tenKhachHang; // null nếu khách vãng lai (chưa đăng nhập)
    private String cauHoiCuoi;
    private LocalDateTime thoiGian;
    private long soLuotChat;
}
