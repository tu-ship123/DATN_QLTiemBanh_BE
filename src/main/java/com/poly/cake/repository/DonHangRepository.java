package com.poly.cake.repository;

import com.poly.cake.entity.DonHang;
import com.poly.cake.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonHangRepository extends JpaRepository<DonHang, Long> {
    
    // Tìm danh sách đơn hàng của một khách hàng, sắp xếp mới nhất lên đầu
    List<DonHang> findByKhachHangOrderByNgayTaoDesc(NguoiDung khachHang);
    
    // Tìm đích danh 1 đơn hàng theo ID và Khách hàng (Dùng để kiểm tra quyền hủy đơn)
    Optional<DonHang> findByIdAndKhachHang(Long id, NguoiDung khachHang);

    // Lọc đơn hàng nâng cao cho Admin
    @Query("SELECT d FROM DonHang d WHERE " +
           "(:trangThai IS NULL OR d.trangThai = :trangThai) AND " +
           "(:nguonDon IS NULL OR d.nguonDon = :nguonDon) AND " +
           "(:tuNgay IS NULL OR d.ngayTao >= :tuNgay) AND " +
           "(:denNgay IS NULL OR d.ngayTao <= :denNgay) " +
           "ORDER BY d.ngayTao DESC")
    List<DonHang> filterAdminOrders(@org.springframework.data.repository.query.Param("trangThai") String trangThai,
                                    @org.springframework.data.repository.query.Param("nguonDon") String nguonDon,
                                    @org.springframework.data.repository.query.Param("tuNgay") java.time.LocalDateTime tuNgay,
                                    @org.springframework.data.repository.query.Param("denNgay") java.time.LocalDateTime denNgay);
}