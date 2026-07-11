package com.poly.cake.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@EnableScheduling // Bật tính năng chạy ngầm tự động
public class BackupService {

    private final JdbcTemplate jdbcTemplate;
    private final JavaMailSender mailSender;

    public BackupService(JdbcTemplate jdbcTemplate, JavaMailSender mailSender) {
        this.jdbcTemplate = jdbcTemplate;
        this.mailSender = mailSender;
    }

    // Cron expression: Giây Phút Giờ Ngày Tháng Thứ (0 0 2 * * ? = 2:00:00 AM mỗi ngày)
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoBackupDatabase() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String backupFileName = "PolyCake_Backup_" + timestamp + ".bak";

        // LƯU Ý: Thư mục C:\Backups phải được tạo sẵn trên máy!
        String backupPath = "C:\\Backups\\" + backupFileName;

        try {
            // Lệnh native của SQL Server để backup
            String sql = "BACKUP DATABASE [QL_TiemBanh] TO DISK = '" + backupPath + "'";
            jdbcTemplate.execute(sql);

            sendEmailReport("Thành công", "Hệ thống đã tự động backup DB vào file: " + backupPath);
            System.out.println("Backup DB thành công: " + backupPath);

        } catch (Exception e) {
            sendEmailReport("Thất bại", "Quá trình backup lúc 2AM gặp lỗi: " + e.getMessage());
            System.err.println("Lỗi Backup DB: " + e.getMessage());
        }
    }

    private void sendEmailReport(String status, String messageBody) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("baonhts02135@gmail.com"); // Đổi thành email người nhận (Lead/Admin)
            message.setSubject("[PolyCake] Báo cáo Auto-Backup 2AM - " + status);
            message.setText(messageBody);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Không thể gửi email báo cáo: " + e.getMessage());
        }
    }
}