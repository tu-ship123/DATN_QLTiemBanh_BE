package com.poly.cake.controller;

import com.poly.cake.dto.StaffCheckinRequest;
import com.poly.cake.dto.ChamCongResponse;
import com.poly.cake.service.ChamCongService;
import com.poly.cake.service.PhanCaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller dành cho nhân viên:
 *   GET  /api/v1/staff/my-schedules            → Lịch ca hôm nay của tôi
 *   GET  /api/v1/staff/my-schedules/week       → Lịch ca trong tuần
 *   POST /api/v1/staff/checkin                 → Check-in
 *   POST /api/v1/staff/checkout/{phanCaId}     → Check-out
 */
@RestController
@RequestMapping("/api/v1/staff")
@PreAuthorize("hasAnyRole('NHAN_VIEN', 'ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Staff - Chấm công", description = "API check-in/check-out và xem lịch ca cho nhân viên")
public class StaffChamCongController {

    private final ChamCongService chamCongService;
    private final PhanCaService   phanCaService;

    // ── Xem lịch ca ──────────────────────────────────────────────────────────

    /**
     * Nhân viên xem lịch ca của mình trong 1 ngày.
     * Không truyền date → hôm nay.
     *
     * Ví dụ: GET /api/v1/staff/my-schedules?date=2025-06-25
     */
    @GetMapping("/my-schedules")
    @Operation(summary = "Xem lịch ca hôm nay",
            description = "Trả về danh sách ca được phân cho nhân viên đang đăng nhập trong ngày chỉ định (mặc định hôm nay)")
    public ResponseEntity<?> getMySchedules(
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(phanCaService.getMySchedules(date));
    }

    /**
     * Nhân viên xem lịch ca trong khoảng thời gian (dùng cho màn hình lịch tuần).
     *
     * Ví dụ: GET /api/v1/staff/my-schedules/week?tuNgay=2025-06-23&denNgay=2025-06-29
     */
    @GetMapping("/my-schedules/week")
    @Operation(summary = "Xem lịch ca cả tuần",
            description = "Trả về danh sách ca của nhân viên trong khoảng ngày chỉ định")
    public ResponseEntity<?> getMySchedulesInRange(
            @RequestParam(required = false) String tuNgay,
            @RequestParam(required = false) String denNgay) {
        return ResponseEntity.ok(phanCaService.getMySchedulesInRange(tuNgay, denNgay));
    }

    // ── Check-in / Check-out ──────────────────────────────────────────────────

    @PostMapping("/checkin")
    @Operation(summary = "Check-in ca làm việc",
            description = "Nhân viên chọn ca đã được phân, ghi nhận giờ vào và tính phút đi trễ")
    public ResponseEntity<?> checkIn(@Valid @RequestBody StaffCheckinRequest request) {
        ChamCongResponse response = chamCongService.checkIn(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout/{phanCaId}")
    @Operation(summary = "Check-out ca làm việc",
            description = "Nhân viên chốt giờ ra, hệ thống tự động đánh dấu về sớm nếu ra trước giờ kết thúc ca")
    public ResponseEntity<?> checkOut(@PathVariable Long phanCaId) {
        ChamCongResponse response = chamCongService.checkOut(phanCaId);
        return ResponseEntity.ok(response);
    }
}
