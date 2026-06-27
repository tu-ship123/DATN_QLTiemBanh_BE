package com.poly.cake.repository;

import com.poly.cake.entity.DiemThuong;
import com.poly.cake.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiemThuongRepository extends JpaRepository<DiemThuong, Long> {

    /** Lịch sử giao dịch điểm của 1 khách, mới nhất lên đầu */
    List<DiemThuong> findByKhachHangOrderByNgayTaoDesc(NguoiDung khachHang);

    /** Tổng điểm hiện có của khách (cộng tất cả giao dịch) */
    @Query("SELECT COALESCE(SUM(d.diemThayDoi), 0) FROM DiemThuong d WHERE d.khachHang = :khachHang")
    Integer tinhTongDiem(@Param("khachHang") NguoiDung khachHang);

    /** Kiểm tra khách đã được cộng điểm cho đơn hàng này chưa (tránh duplicate) */
    boolean existsByKhachHangAndDonHangIdAndLoaiGiaoDich(NguoiDung khachHang, Long donHangId, String loaiGiaoDich);
}
