package com.poly.cake.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cau_hinh_he_thong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CauHinhHeThong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, columnDefinition = "NVARCHAR(100)")
    private String khoaCauHinh;

    @Column(nullable = false, columnDefinition = "NVARCHAR(500)")
    private String giaTri;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String moTa;
}
