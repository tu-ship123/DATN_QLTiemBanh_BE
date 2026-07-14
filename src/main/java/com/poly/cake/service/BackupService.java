package com.poly.cake.service;

import com.poly.cake.entity.NguoiDung;
import com.poly.cake.entity.NhatKyHeThong;
import com.poly.cake.repository.NguoiDungRepository;
import com.poly.cake.repository.NhatKyHeThongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@EnableScheduling // Bật tính năng chạy ngầm tự động
public class BackupService {

    private final JdbcTemplate jdbcTemplate;
    private final JavaMailSender mailSender;
    private final NhatKyHeThongRepository nhatKyHeThongRepository;
    private final NguoiDungRepository nguoiDungRepository;

    private static final String HANH_DONG_BACKUP = "BACKUP_DATABASE";

    // Có thể cấu hình qua application.yml: app.backup.thu-muc: /var/backups/polycake
    // Mặc định dùng thư mục tương đối "backups/" trong thư mục chạy app để không vỡ
    // trên Linux/server thật (trước đây hard-code "C:\\Backups\\" chỉ chạy được trên Windows).
    @Value("${app.backup.thu-muc:backups}")
    private String thuMucBackup;

    // Cron expression: Giây Phút Giờ Ngày Tháng Thứ (0 0 2 * * ? = 2:00:00 AM mỗi ngày)
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoBackupDatabase() {
        thucHienBackup(null);
    }

    // API thủ công - Admin bấm nút "Backup ngay" trên trang Backup
    public NhatKyHeThong backupNgay(String emailNguoiThucHien) {
        Long nguoiDungId = null;
        if (emailNguoiThucHien != null) {
            nguoiDungId = nguoiDungRepository.findByEmail(emailNguoiThucHien).map(NguoiDung::getId).orElse(null);
        }
        return thucHienBackup(nguoiDungId);
    }

    private NhatKyHeThong thucHienBackup(Long nguoiThucHienId) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String backupFileName = "PolyCake_Backup_" + timestamp + ".bak";

        File thuMuc = new File(thuMucBackup);
        if (!thuMuc.exists()) {
            thuMuc.mkdirs();
        }
        String backupPath = new File(thuMuc, backupFileName).getAbsolutePath();

        NhatKyHeThong.NhatKyHeThongBuilder logBuilder = NhatKyHeThong.builder()
                .hanhDong(HANH_DONG_BACKUP)
                .tenBang("DATABASE");

        if (nguoiThucHienId != null) {
            logBuilder.nguoiDung(nguoiDungRepository.findById(nguoiThucHienId).orElse(null));
        }

        try {
            String sql = "BACKUP DATABASE [QL_TiemBanh] TO DISK = '" + backupPath + "'";
            jdbcTemplate.execute(sql);

            NhatKyHeThong log = logBuilder
                    .giaTriMoi("THANH_CONG|" + backupFileName)
                    .build();
            log = nhatKyHeThongRepository.save(log);

            sendEmailReport("Thành công", "Hệ thống đã backup DB vào file: " + backupPath);
            return log;
        } catch (Exception e) {
            NhatKyHeThong log = logBuilder
                    .giaTriMoi("THAT_BAI|" + e.getMessage())
                    .build();
            log = nhatKyHeThongRepository.save(log);

            sendEmailReport("Thất bại", "Quá trình backup gặp lỗi: " + e.getMessage());
            return log;
        }
    }

    // Lịch sử backup (thủ công + tự động) - mới nhất lên đầu
    public List<NhatKyHeThong> getLichSuBackup() {
        return nhatKyHeThongRepository.findByHanhDongInOrderByNgayTaoDesc(List.of(HANH_DONG_BACKUP));
    }

    private void sendEmailReport(String status, String messageBody) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("admin@chocopine.vn"); // Đổi thành email người nhận (Lead/Admin)
            message.setSubject("[PolyCake] Báo cáo Backup DB - " + status);
            message.setText(messageBody);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Không thể gửi email báo cáo: " + e.getMessage());
        }
    }
}
