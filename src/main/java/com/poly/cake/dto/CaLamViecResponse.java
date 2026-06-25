package com.poly.cake.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;

/**
 * DTO trả về danh sách ca làm việc cho FE dropdown.
 */
@Data
@Builder
public class CaLamViecResponse {
    private Long id;
    private String tenCa;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;
    private Boolean hoatDong;
}
