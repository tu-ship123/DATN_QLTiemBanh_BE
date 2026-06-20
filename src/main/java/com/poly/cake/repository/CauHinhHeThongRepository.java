package com.poly.cake.repository;

import com.poly.cake.entity.CauHinhHeThong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CauHinhHeThongRepository extends JpaRepository<CauHinhHeThong, Long> {
    // Sửa thành:
    Optional<CauHinhHeThong> findByKhoaCauHinh(String khoaCauHinh);
}