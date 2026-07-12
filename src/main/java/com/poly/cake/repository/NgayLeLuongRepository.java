package com.poly.cake.repository;

import com.poly.cake.entity.NgayLeLuong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface NgayLeLuongRepository extends JpaRepository<NgayLeLuong, Long> {

    Optional<NgayLeLuong> findByNgayLeAndHoatDongTrue(LocalDate ngayLe);

    boolean existsByNgayLe(LocalDate ngayLe);

    List<NgayLeLuong> findAllByOrderByNgayLeAsc();
}