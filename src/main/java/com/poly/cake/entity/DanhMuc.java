package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "danh_muc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DanhMuc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String tenDanhMuc;

    private String moTa;

    @Column(length = 500)
    private String anhDaiDien;

    private Boolean hoatDong = true;
}