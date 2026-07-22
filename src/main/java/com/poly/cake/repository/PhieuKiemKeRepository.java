package com.poly.cake.repository;

import com.poly.cake.entity.PhieuKiemKe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhieuKiemKeRepository extends JpaRepository<PhieuKiemKe, Long> {
    List<PhieuKiemKe> findAllByOrderByNgayKiemKeDesc();
}