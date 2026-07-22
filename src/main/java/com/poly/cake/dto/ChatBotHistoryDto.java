package com.poly.cake.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatBotHistoryDto {
    private Long id;
    private String cauHoi;
    private String traLoi;
    private LocalDateTime ngayTao;
}
