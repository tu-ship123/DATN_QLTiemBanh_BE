package com.poly.cake.repository;

import com.poly.cake.entity.DanhMuc;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DanhMucRepository extends JpaRepository<DanhMuc, Long> {

    boolean existsByTenDanhMuc(String tenDanhMuc);

}