package com.poly.cake.repository;

import com.poly.cake.entity.MaGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaGiamGiaRepository extends JpaRepository<MaGiamGia, Long> {

    boolean existsByMaCode(String maCode);

    Optional<MaGiamGia> findByMaCode(String maCode);
}