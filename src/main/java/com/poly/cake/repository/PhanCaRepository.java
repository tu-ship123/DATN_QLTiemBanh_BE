package com.poly.cake.repository;

import com.poly.cake.entity.PhanCa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhanCaRepository extends JpaRepository<PhanCa, Long> {
    List<PhanCa> findByNhanVienIdAndNgayLamViec(Long nhanVienId, LocalDate ngayLamViec);
    Optional<PhanCa> findByIdAndNhanVienId(Long id, Long nhanVienId);
}