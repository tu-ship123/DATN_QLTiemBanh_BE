package com.poly.cake.repository;

import com.poly.cake.entity.PhuThuDonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PhuThuDonHangRepository extends JpaRepository<PhuThuDonHang, Long> {

    @Query("SELECT p FROM PhuThuDonHang p WHERE p.hoatDong = true " +
            "AND :ngay >= p.ngayBatDau AND :ngay <= p.ngayKetThuc")
    List<PhuThuDonHang> findApDungTheoNgay(@Param("ngay") LocalDate ngay);

    List<PhuThuDonHang> findAllByOrderByNgayBatDauDesc();
}