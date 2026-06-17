package com.poly.cake.repository;

import com.poly.cake.entity.LamMoiToken;
import com.poly.cake.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LamMoiTokenRepository extends JpaRepository<LamMoiToken, Long> {
    Optional<LamMoiToken> findByToken(String token);
    void deleteByNguoiDung(NguoiDung nguoiDung);
}