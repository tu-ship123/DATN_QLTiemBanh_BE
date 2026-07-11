package com.poly.cake.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "thiet_ke_yeu_thich")
@Data
public class ThietKeYeuThich {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "khach_hang_id", nullable = false)
    private Long khachHangId;

    @Column(name = "ten_thiet_ke", length = 200)
    private String tenThietKe;

    @Column(name = "thiet_ke_banh_json", nullable = false, columnDefinition = "TEXT")
    private String thietKeBanhJson;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao = LocalDateTime.now();
}