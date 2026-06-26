package com.poly.cake.repository;

import com.poly.cake.entity.PhuKienTrangTri;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhuKienTrangTriRepository extends JpaRepository<PhuKienTrangTri, Long> {
}