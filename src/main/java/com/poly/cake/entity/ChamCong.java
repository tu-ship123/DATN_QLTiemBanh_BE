package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cham_cong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChamCong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phan_ca_id", nullable = false, unique = true)
    private PhanCa phanCa;

    private LocalDateTime gioVao;

    private LocalDateTime gioRa;

    private Integer phutDiTre = 0;

    @Column(nullable = false)
    private String trangThai = "DUNG_GIO"; // DUNG_GIO, DI_TRE, VANG_MAT, VE_SOM

    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}