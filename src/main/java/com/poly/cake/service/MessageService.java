package com.poly.cake.service;

import com.poly.cake.dto.ConversationSummaryDto;
import com.poly.cake.dto.TinNhanDto;
import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.TinNhan;
import com.poly.cake.exception.ResourceNotFoundException;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.TinNhanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final TinNhanRepository tinNhanRepository;
    private final NguoiDungRepository nguoiDungRepository;

    // ── ADMIN: Hộp thư đến - danh sách hội thoại, mới nhất lên đầu ──
    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> getConversations() {
        List<Long> khachHangIds = tinNhanRepository.findDistinctKhachHangIdsOrderByLatestMessage();
        return khachHangIds.stream().map(id -> {
            TinNhan latest = tinNhanRepository.findTopByKhachHangIdOrderByNgayTaoDesc(id);
            long chuaDoc = tinNhanRepository.countByKhachHangIdAndTuCuaHangFalseAndDaDocFalse(id);
            return new ConversationSummaryDto(
                    id,
                    latest.getKhachHang().getHoTen(),
                    latest.getNoiDung(),
                    latest.getNgayTao(),
                    chuaDoc
            );
        }).collect(Collectors.toList());
    }

    // ── Lấy toàn bộ tin nhắn của 1 khách hàng (dùng cho cả admin xem lẫn khách xem của chính mình) ──
    @Transactional(readOnly = true)
    public List<TinNhanDto> getMessages(Long khachHangId) {
        return tinNhanRepository.findByKhachHangIdOrderByNgayTaoAsc(khachHangId).stream()
                .map(t -> new TinNhanDto(t.getId(), t.getNoiDung(), t.isTuCuaHang(), t.getNgayTao(),
                        t.getNguoiGui() != null ? t.getNguoiGui().getHoTen() : null))
                .collect(Collectors.toList());
    }

    // ── Gửi tin nhắn: nguoiGuiId là người thực sự gõ tin; khachHangId là chủ cuộc hội thoại ──
    @Transactional
    public TinNhanDto guiTinNhan(Long khachHangId, Long nguoiGuiId, String noiDung, boolean tuCuaHang) {
        NguoiDung khachHang = nguoiDungRepository.findById(khachHangId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng!"));
        NguoiDung nguoiGui = nguoiDungRepository.findById(nguoiGuiId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người gửi!"));

        TinNhan tinNhan = TinNhan.builder()
                .khachHang(khachHang)
                .nguoiGui(nguoiGui)
                .noiDung(noiDung)
                .tuCuaHang(tuCuaHang)
                .daDoc(tuCuaHang) // tin từ cửa hàng gửi thì coi như đã đọc với chính cửa hàng
                .build();
        tinNhan = tinNhanRepository.save(tinNhan);

        return new TinNhanDto(tinNhan.getId(), tinNhan.getNoiDung(), tinNhan.isTuCuaHang(),
                tinNhan.getNgayTao(), nguoiGui.getHoTen());
    }

    @Transactional
    public void danhDauDaDoc(Long khachHangId) {
        tinNhanRepository.markAllAsReadForKhachHang(khachHangId);
    }
}
