package com.poly.cake.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PhanCaRequest {
    @NotNull(message = "Nhân viên không được để trống")
    private Long nhanVienId;

    @NotNull(message = "Ca làm việc không được để trống")
    private Long caLamViecId;

    @NotNull(message = "Ngày làm việc không được để trống")
    private LocalDate ngayLamViec;

    private String ghiChu;
}