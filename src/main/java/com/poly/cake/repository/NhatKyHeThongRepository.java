package com.poly.cake.repository;

import com.poly.cake.entity.NhatKyHeThong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NhatKyHeThongRepository extends JpaRepository<NhatKyHeThong, Long> {
    List<NhatKyHeThong> findByHanhDongInOrderByNgayTaoDesc(List<String> hanhDongs);
}