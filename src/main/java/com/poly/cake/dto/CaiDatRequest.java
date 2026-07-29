package com.poly.cake.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CaiDatRequest {
    @NotBlank(message = "Giá trị không được để trống")
    private String giaTri;
}