package com.poly.cake.repository;

import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.ThongBao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThongBaoRepository extends JpaRepository<ThongBao, Long> {

    // Danh sách thông báo của 1 người dùng, mới nhất lên đầu
    List<ThongBao> findByNguoiDungOrderByNgayTaoDesc(NguoiDung nguoiDung);

    // Đếm số thông báo chưa đọc
    long countByNguoiDungAndDaDocFalse(NguoiDung nguoiDung);

    // Kiểm tra đã có cảnh báo TON_KHO cho sản phẩm này gửi tới người dùng này
    // trong khoảng thời gian gần đây chưa (dùng để chống spam thông báo trùng lặp)
    @Query("SELECT COUNT(t) > 0 FROM ThongBao t " +
           "WHERE t.nguoiDung = :nguoiDung " +
           "AND t.loaiThongBao = 'TON_KHO' " +
           "AND t.tieuDe = :tieuDe " +
           "AND t.ngayTao >= :tuThoiDiem")
    boolean existsCanhBaoTrungGanDay(
            @Param("nguoiDung") NguoiDung nguoiDung,
            @Param("tieuDe") String tieuDe,
            @Param("tuThoiDiem") java.time.LocalDateTime tuThoiDiem
    );
}