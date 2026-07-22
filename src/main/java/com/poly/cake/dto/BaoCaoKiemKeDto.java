package com.poly.cake.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaoCaoKiemKeDto {
    private Long id;
    private Long sanPhamId;
    private String tenSanPham;
    private Integer tonHeThong;
    private Integer tonThucTe;
    private Integer chenhLech;
    private String lyDo;
    private String nguoiThucHien;
    private LocalDateTime ngayKiemKe;
}