package com.poly.cake.repository;

import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.ThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ThanhToanRepository extends JpaRepository<ThanhToan, Long> {
    Optional<ThanhToan> findByDonHang(DonHang donHang);
}