package com.poly.cake.service;

import com.poly.cake.dto.DanhMucDto;
import com.poly.cake.entity.DanhMuc;
import com.poly.cake.repository.DanhMucRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminDanhMucService {

    @Autowired
    private DanhMucRepository danhMucRepository;

    // 1. GET ALL
    public List<DanhMucDto.Response> getAllDanhMuc() {
        return danhMucRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // 2. GET BY ID
    public DanhMucDto.Response getDanhMucById(Long id) {

        DanhMuc danhMuc = danhMucRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy danh mục"));

        return mapToResponseDto(danhMuc);
    }

    // CREATE
@Transactional
public DanhMucDto.Response createDanhMuc(DanhMucDto.Request request) {

    if (danhMucRepository.existsByTenDanhMuc(request.getTenDanhMuc())) {
        throw new RuntimeException("Tên danh mục đã tồn tại");
    }

    DanhMuc danhMuc = new DanhMuc();
    danhMuc.setTenDanhMuc(request.getTenDanhMuc());
    danhMuc.setMoTa(request.getMoTa());
    danhMuc.setAnhDaiDien(request.getAnhDaiDien());
    danhMuc.setHoatDong(request.getHoatDong() != null ? request.getHoatDong() : true);

    return mapToResponseDto(danhMucRepository.save(danhMuc));
}

    // 4. UPDATE
    @Transactional
    public DanhMucDto.Response updateDanhMuc(
            Long id,
            DanhMucDto.Request request) {

        DanhMuc danhMuc = danhMucRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy danh mục"));

        danhMuc.setTenDanhMuc(request.getTenDanhMuc());
        danhMuc.setMoTa(request.getMoTa());
        danhMuc.setAnhDaiDien(request.getAnhDaiDien());
        danhMuc.setHoatDong(request.getHoatDong());

        DanhMuc updated = danhMucRepository.save(danhMuc);

        return mapToResponseDto(updated);
    }

    // 5. DELETE
    @Transactional
    public void deleteDanhMuc(Long id) {

        DanhMuc danhMuc = danhMucRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy danh mục"));

        danhMucRepository.delete(danhMuc);
    }


    private DanhMucDto.Response mapToResponseDto(
            DanhMuc danhMuc) {

        DanhMucDto.Response dto =
                new DanhMucDto.Response();

        dto.setId(danhMuc.getId());
        dto.setTenDanhMuc(danhMuc.getTenDanhMuc());
        dto.setMoTa(danhMuc.getMoTa());
        dto.setAnhDaiDien(danhMuc.getAnhDaiDien());
        dto.setHoatDong(danhMuc.getHoatDong());

        return dto;
    }
}