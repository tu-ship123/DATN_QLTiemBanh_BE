package com.poly.cake.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO trả về khi lấy thông tin phân ca.
 * Dùng cho cả admin (xem lịch tất cả NV) và nhân viên (xem lịch của mình).
 */
@Data
@Builder
public class PhanCaResponse {
    private Long id;
    private LocalDate ngayLamViec;
    private String trangThai;
    private String ghiChu;
    private LocalDateTime ngayTao;

    // Thông tin ca làm việc (nested)
    private Long caLamViecId;
    private String tenCa;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;

    // Thông tin nhân viên (nested)
    private Long nhanVienId;
    private String tenNhanVien;
    private String emailNhanVien;
}
