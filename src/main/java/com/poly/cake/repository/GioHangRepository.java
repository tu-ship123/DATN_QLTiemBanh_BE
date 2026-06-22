package com.poly.cake.repository;

import com.poly.cake.entity.GioHang;
import com.poly.cake.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GioHangRepository extends JpaRepository<GioHang, Long> {
    Optional<GioHang> findByKhachHang(NguoiDung khachHang);
    Optional<GioHang> findByKhachHangEmail(String email);
}
