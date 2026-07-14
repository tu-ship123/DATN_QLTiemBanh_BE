package com.poly.cake.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TinNhanDto {
    private Long id;
    private String noiDung;
    private boolean tuCuaHang;
    private LocalDateTime ngayTao;
    private String tenNguoiGui;
}
