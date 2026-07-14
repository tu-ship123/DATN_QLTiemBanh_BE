package com.poly.cake.repository;

import com.poly.cake.entity.TinNhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TinNhanRepository extends JpaRepository<TinNhan, Long> {

    List<TinNhan> findByKhachHangIdOrderByNgayTaoAsc(Long khachHangId);

    long countByKhachHangIdAndTuCuaHangFalseAndDaDocFalse(Long khachHangId);

    // Danh sách khách hàng đã từng nhắn tin, kèm ID tin nhắn mới nhất của mỗi người
    // (dùng để build "Hộp thư đến" - mỗi khách 1 dòng hội thoại)
    @Query("SELECT t.khachHang.id FROM TinNhan t GROUP BY t.khachHang.id ORDER BY MAX(t.ngayTao) DESC")
    List<Long> findDistinctKhachHangIdsOrderByLatestMessage();

    TinNhan findTopByKhachHangIdOrderByNgayTaoDesc(Long khachHangId);

    @Query("UPDATE TinNhan t SET t.daDoc = true WHERE t.khachHang.id = :khachHangId AND t.tuCuaHang = false")
    @org.springframework.data.jpa.repository.Modifying
    void markAllAsReadForKhachHang(@Param("khachHangId") Long khachHangId);
}
