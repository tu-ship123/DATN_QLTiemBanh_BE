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

    @Column(nullable = false, columnDefinition = "NVARCHAR(150)")
    private String tenDanhMuc;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String moTa;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String anhDaiDien;

    private Boolean hoatDong = true;
}