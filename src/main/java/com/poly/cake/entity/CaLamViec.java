package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "ca_lam_viec")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaLamViec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String tenCa;

    @Column(nullable = false)
    private LocalTime gioBatDau;

    @Column(nullable = false)
    private LocalTime gioKetThuc;

    private Boolean hoatDong = true;
}
