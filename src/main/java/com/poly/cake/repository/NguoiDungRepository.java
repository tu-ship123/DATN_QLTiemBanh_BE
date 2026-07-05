package com.poly.cake.repository;

import com.poly.cake.entity.NguoiDung;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NguoiDungRepository extends JpaRepository<NguoiDung, Long> {
    Optional<NguoiDung> findByEmail(String email);

    // Dùng để kiểm tra trùng email khi đăng ký / tạo tài khoản (khách hàng, nhân viên)
    boolean existsByEmail(String email);

    // Dùng để kiểm tra trùng số điện thoại khi đăng ký / tạo tài khoản
    boolean existsBySoDienThoai(String soDienThoai);

    // Tìm khách theo số điện thoại (dùng cho POS cộng điểm offline)
    Optional<NguoiDung> findBySoDienThoaiAndQuyen(String soDienThoai, String quyen);

    // Lấy danh sách người dùng theo nhiều quyền (VD: ADMIN, NHAN_VIEN) và đang hoạt
    // động - dùng để gửi cảnh báo tồn kho thấp / bán vượt tồn kho (InventoryService)
    List<NguoiDung> findByQuyenInAndTrangThai(List<String> quyen, String trangThai);
    @Query("SELECT COUNT(u) FROM NguoiDung u WHERE u.quyen = 'KHACH_HANG' AND u.ngayTao >= :startDate AND u.ngayTao < :endDate")
    Long countKhachMoiByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}