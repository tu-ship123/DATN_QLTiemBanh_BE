package com.poly.cake.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PhanCaRequest {
    private Long nhanVienId;
    private Long caLamViecId;
    private LocalDate ngayLamViec;
    private String ghiChu;
}