package com.poly.cake.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryDto {
    private Long khachHangId;
    private String tenKhachHang;
    private String tinNhanCuoi;
    private LocalDateTime thoiGian;
    private long soTinChuaDoc;
}
