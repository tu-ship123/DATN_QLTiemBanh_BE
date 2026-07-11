package com.poly.cake.repository;

import com.poly.cake.entity.CauHinhNgayLe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CauHinhNgayLeRepository extends JpaRepository<CauHinhNgayLe, Long> {
    // Tìm kiếm thông tin cấu hình theo ngày cụ thể
    Optional<CauHinhNgayLe> findByNgay(LocalDate ngay);
}