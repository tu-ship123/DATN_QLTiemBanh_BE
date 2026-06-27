package com.poly.cake.repository;

import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.VoucherKhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherKhachHangRepository extends JpaRepository<VoucherKhachHang, Long> {

    /** Tất cả voucher của 1 khách, mới nhất lên đầu */
    List<VoucherKhachHang> findByKhachHangOrderByNgayTaoDesc(NguoiDung khachHang);

    /** Chỉ lấy voucher còn hiệu lực */
    List<VoucherKhachHang> findByKhachHangAndTrangThaiAndNgayHetHanAfter(
            NguoiDung khachHang, String trangThai, LocalDateTime now);

    /** Tìm 1 voucher theo id và chủ sở hữu (dùng khi checkout) */
    Optional<VoucherKhachHang> findByIdAndKhachHang(Long id, NguoiDung khachHang);
}
