package com.poly.cake.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StaffCheckinRequest {
    @NotNull(message = "Phân ca không được để trống")
    private Long phanCaId; // ID phân ca muốn check-in
}