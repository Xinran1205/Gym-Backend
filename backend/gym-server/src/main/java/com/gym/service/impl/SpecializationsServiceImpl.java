package com.gym.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.dao.SpecializationsDao;
import com.gym.dto.SpecializationDTO;
import com.gym.entity.Specializations;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.service.SpecializationsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SpecializationsServiceImpl
        extends ServiceImpl<SpecializationsDao, Specializations>
        implements SpecializationsService {

    @Override
    @Transactional
    public Specializations addSpecialization(SpecializationDTO dto) {
        Specializations spec = Specializations.builder()
                .description(dto.getDescription().trim())
                .build();
        boolean saved = this.save(spec);
        if (!saved) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Failed to add specialization.");
        }
        return spec;
    }

    @Override
    @Transactional
    public void deleteSpecialization(Long specializationId) {
        boolean removed = this.removeById(specializationId);
        if (!removed) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Specialization not found or already deleted.");
        }
    }

    @Override
    public List<Specializations> listAllSpecializations() {
        return this.list();
    }
}
