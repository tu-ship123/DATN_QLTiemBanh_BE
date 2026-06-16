package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ma_giam_gia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaGiamGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String maCode;

    @Column(nullable = false)
    private String loaiGiamGia; // PHAN_TRAM, SO_TIEN_CO_DINH

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal giaTriGiam;

    @Column(precision = 12, scale = 2)
    private BigDecimal donHangToiThieu;

    private Integer soLuotToiDa;

    private Integer soLuotDaDung = 0;

    @Column(nullable = false)
    private LocalDateTime ngayHetHan;

    private Boolean hoatDong = true;
}
