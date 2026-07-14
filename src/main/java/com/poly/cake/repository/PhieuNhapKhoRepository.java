package com.poly.cake.repository;

import com.poly.cake.entity.PhieuNhapKho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhieuNhapKhoRepository extends JpaRepository<PhieuNhapKho, Long> {
    List<PhieuNhapKho> findAllByOrderByNgayTaoDesc();
}