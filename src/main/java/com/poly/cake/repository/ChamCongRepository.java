package com.poly.cake.repository;

import com.poly.cake.entity.ChamCong;
import com.poly.cake.entity.PhanCa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChamCongRepository extends JpaRepository<ChamCong, Long> {
    Optional<ChamCong> findByPhanCa(PhanCa phanCa);
    boolean existsByPhanCa(PhanCa phanCa);
}