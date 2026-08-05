package com.poly.cake.repository;

import com.poly.cake.entity.ChamCong;
import com.poly.cake.entity.PhanCa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ChamCongRepository extends JpaRepository<ChamCong, Long> {

    Optional<ChamCong> findByPhanCa(PhanCa phanCa);
    boolean existsByPhanCa(PhanCa phanCa);
    @Query("SELECT cc FROM ChamCong cc WHERE cc.phanCa.ngayLamViec = :ngay AND cc.loaiBaoCao IS NOT NULL")
    List<ChamCong> findByNgayLamViecAndCoLoaiBaoCao(@Param("ngay") LocalDate ngay);

    // [FIX] Trước đây bắt buộc cả gioVao LẪN gioRa đều phải có mới lấy ra ->
    // nhân viên nào có ca CHƯA check-out (gioRa = null, vd ca đang làm dở, hoặc
    // quên bấm check-out) sẽ bị loại khỏi bảng lương hoàn toàn, dù đã có dữ liệu
    // chấm công (check-in) trong DB. Giờ chỉ cần gioVao (luôn có ngay từ lúc
    // check-in) để nhân viên luôn xuất hiện trong bảng lương; ca chưa check-out
    // sẽ không được tính giờ làm ở tầng service (xem BaoCaoService) chứ không bị
    // loại bỏ luôn cả nhân viên.
    @Query("SELECT c FROM ChamCong c JOIN FETCH c.phanCa pc JOIN FETCH pc.nhanVien nv " +
            "WHERE MONTH(pc.ngayLamViec) = :thang AND YEAR(pc.ngayLamViec) = :nam " +
            "AND c.gioVao IS NOT NULL")
    List<ChamCong> findByThangAndNam(@Param("thang") int thang, @Param("nam") int nam);

}