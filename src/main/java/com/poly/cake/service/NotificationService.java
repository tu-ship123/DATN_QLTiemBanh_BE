package com.poly.cake.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    // 1. BROADCAST: Gắn loa phường (Thông báo cho toàn bộ Nhân viên/Admin)
    // Dùng khi có khách đặt đơn mới
    public void notifyNewOrderToAdmins(String thongBao) {
        messagingTemplate.convertAndSend("/topic/admin/orders", thongBao);
    }

    // 2. PUSH NOTIFICATION: Nhắn tin riêng (Thông báo trạng thái đơn cho 1 khách hàng cụ thể)
    // Dùng khi Admin duyệt đơn/hủy đơn của khách
    public void notifyOrderStatusToUser(String emailKhachHang, String thongBao) {
        // User sẽ lắng nghe ở endpoint: /user/queue/notifications
        messagingTemplate.convertAndSendToUser(emailKhachHang, "/queue/notifications", thongBao);
    }
}