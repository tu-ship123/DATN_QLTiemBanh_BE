package com.poly.cake.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChamCongResponse {
    private Long id;
    private Long phanCaId;
    private String tenCa;
    private String ngayLamViec;
    private LocalDateTime gioVao;
    private LocalDateTime gioRa;
    private Integer phutDiTre;
    private String trangThai;
}