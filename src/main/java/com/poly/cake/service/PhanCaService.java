package com.poly.cake.service;

import com.poly.cake.dto.CaLamViecRequest;
import com.poly.cake.dto.CaLamViecResponse;
import com.poly.cake.dto.PhanCaRequest;
import com.poly.cake.dto.PhanCaResponse;
import com.poly.cake.dto.ChamCongResponse;
import com.poly.cake.entity.CaLamViec;
import com.poly.cake.entity.ChamCong;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.PhanCa;
import com.poly.cake.repository.CaLamViecRepository;
import com.poly.cake.repository.ChamCongRepository;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.PhanCaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhanCaService {

    private final PhanCaRepository phanCaRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final CaLamViecRepository caLamViecRepository;
    private final ChamCongRepository chamCongRepository;

    // ── Lấy nhân viên hiện tại từ Security Context ────────────────────────────
    private NguoiDung getNhanVienHienTai() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADMIN: Quản lý Ca Làm Việc (CRUD)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Lấy danh sách tất cả ca làm việc đang hoạt động.
     * Dùng cho FE dropdown khi phân ca.
     */
    public List<CaLamViecResponse> getAllCaLamViec() {
        return caLamViecRepository.findAll().stream()
                .map(this::mapCaToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tạo ca làm việc mới.
     */
    @Transactional
    public CaLamViecResponse createCaLamViec(CaLamViecRequest request) {
        if (request.getGioKetThuc().isBefore(request.getGioBatDau()) ||
            request.getGioKetThuc().equals(request.getGioBatDau())) {
            throw new RuntimeException("Giờ kết thúc phải sau giờ bắt đầu");
        }

        CaLamViec ca = CaLamViec.builder()
                .tenCa(request.getTenCa())
                .gioBatDau(request.getGioBatDau())
                .gioKetThuc(request.getGioKetThuc())
                .hoatDong(request.getHoatDong() != null ? request.getHoatDong() : true)
                .build();

        return mapCaToResponse(caLamViecRepository.save(ca));
    }

    /**
     * Cập nhật thông tin ca làm việc.
     */
    @Transactional
    public CaLamViecResponse updateCaLamViec(Long id, CaLamViecRequest request) {
        CaLamViec ca = caLamViecRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc"));

        if (request.getGioKetThuc().isBefore(request.getGioBatDau()) ||
            request.getGioKetThuc().equals(request.getGioBatDau())) {
            throw new RuntimeException("Giờ kết thúc phải sau giờ bắt đầu");
        }

        ca.setTenCa(request.getTenCa());
        ca.setGioBatDau(request.getGioBatDau());
        ca.setGioKetThuc(request.getGioKetThuc());
        if (request.getHoatDong() != null) {
            ca.setHoatDong(request.getHoatDong());
        }

        return mapCaToResponse(caLamViecRepository.save(ca));
    }

    /**
     * Xoá (vô hiệu hoá) ca làm việc — soft delete.
     */
    @Transactional
    public void deleteCaLamViec(Long id) {
        CaLamViec ca = caLamViecRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc"));
        ca.setHoatDong(false);
        caLamViecRepository.save(ca);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADMIN: Phân Ca cho Nhân Viên
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Tạo lịch phân ca mới — có kiểm tra trùng ca.
     */
    @Transactional
    public PhanCaResponse createPhanCa(PhanCaRequest request) {
        NguoiDung nhanVien = nguoiDungRepository.findById(request.getNhanVienId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        CaLamViec caLamViec = caLamViecRepository.findById(request.getCaLamViecId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc"));

        if (!Boolean.TRUE.equals(caLamViec.getHoatDong())) {
            throw new RuntimeException("Ca làm việc này đã bị vô hiệu hoá");
        }

        // Kiểm tra trùng ca
        boolean trung = phanCaRepository.existsByNhanVienIdAndCaLamViecIdAndNgayLamViec(
                request.getNhanVienId(), request.getCaLamViecId(), request.getNgayLamViec()
        );
        if (trung) {
            throw new RuntimeException("Nhân viên này đã được phân ca " + caLamViec.getTenCa()
                    + " vào ngày " + request.getNgayLamViec() + " rồi");
        }

        PhanCa phanCa = PhanCa.builder()
                .nhanVien(nhanVien)
                .caLamViec(caLamViec)
                .ngayLamViec(request.getNgayLamViec())
                .trangThai("DA_LAP")
                .ghiChu(request.getGhiChu())
                .build();

        return mapPhanCaToResponse(phanCaRepository.save(phanCa));
    }

    /**
     * Admin xem lịch phân ca theo ngày (để quản lý).
     */
    public List<PhanCaResponse> getPhanCaTheoNgay(LocalDate ngay) {
        LocalDate targetDate = ngay != null ? ngay : LocalDate.now();
        return phanCaRepository.findAllByNgay(targetDate).stream()
                .map(this::mapPhanCaToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Huỷ phân ca.
     */
    @Transactional
    public PhanCaResponse huyPhanCa(Long id) {
        PhanCa phanCa = phanCaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phân ca"));

        if ("XAC_NHAN".equals(phanCa.getTrangThai())) {
            throw new RuntimeException("Không thể huỷ ca đã xác nhận (nhân viên đã check-in)");
        }

        phanCa.setTrangThai("DA_HUY");
        return mapPhanCaToResponse(phanCaRepository.save(phanCa));
    }

    /**
     * Lấy danh sách chấm công (admin).
     */
    public List<ChamCong> getAllChamCong() {
        return chamCongRepository.findAll();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NHÂN VIÊN: Xem lịch ca của mình
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Nhân viên lấy lịch ca của mình theo ngày.
     * Nếu không truyền date → trả về hôm nay.
     *
     * Endpoint: GET /api/v1/staff/my-schedules?date=2025-06-25
     */
    public List<PhanCaResponse> getMySchedules(String dateStr) {
        NguoiDung nhanVien = getNhanVienHienTai();
        LocalDate ngay = (dateStr != null && !dateStr.isBlank())
                ? LocalDate.parse(dateStr)
                : LocalDate.now();

        return phanCaRepository
                .findByNhanVienIdAndNgayWithCa(nhanVien.getId(), ngay)
                .stream()
                .map(this::mapPhanCaToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Nhân viên lấy lịch ca của mình trong một tuần.
     *
     * Endpoint: GET /api/v1/staff/my-schedules/week?tuNgay=2025-06-23&denNgay=2025-06-29
     */
    public List<PhanCaResponse> getMySchedulesInRange(String tuNgayStr, String denNgayStr) {
        NguoiDung nhanVien = getNhanVienHienTai();
        LocalDate tuNgay  = (tuNgayStr  != null) ? LocalDate.parse(tuNgayStr)  : LocalDate.now();
        LocalDate denNgay = (denNgayStr != null) ? LocalDate.parse(denNgayStr) : tuNgay.plusDays(6);

        return phanCaRepository
                .findByNhanVienIdAndDateRange(nhanVien.getId(), tuNgay, denNgay)
                .stream()
                .map(this::mapPhanCaToResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER: Map entity → DTO
    // ═══════════════════════════════════════════════════════════════════════════

    private CaLamViecResponse mapCaToResponse(CaLamViec ca) {
        return CaLamViecResponse.builder()
                .id(ca.getId())
                .tenCa(ca.getTenCa())
                .gioBatDau(ca.getGioBatDau())
                .gioKetThuc(ca.getGioKetThuc())
                .hoatDong(ca.getHoatDong())
                .build();
    }

    public PhanCaResponse mapPhanCaToResponse(PhanCa p) {
        return PhanCaResponse.builder()
                .id(p.getId())
                .ngayLamViec(p.getNgayLamViec())
                .trangThai(p.getTrangThai())
                .ghiChu(p.getGhiChu())
                .ngayTao(p.getNgayTao())
                // Ca làm việc
                .caLamViecId(p.getCaLamViec().getId())
                .tenCa(p.getCaLamViec().getTenCa())
                .gioBatDau(p.getCaLamViec().getGioBatDau())
                .gioKetThuc(p.getCaLamViec().getGioKetThuc())
                // Nhân viên
                .nhanVienId(p.getNhanVien().getId())
                .tenNhanVien(p.getNhanVien().getHoTen())
                .emailNhanVien(p.getNhanVien().getEmail())
                .build();
    }
}
