package com.gym.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.dto.SpecializationDTO;
import com.gym.entity.Specializations;

import java.util.List;

public interface SpecializationsService extends IService<Specializations> {

    /**
     * 新增一个 Specialization
     */
    Specializations addSpecialization(SpecializationDTO dto);

    /**
     * 删除一个 Specialization
     */
    void deleteSpecialization(Long specializationId);

    /**
     * 列出所有 Specializations
     */
    List<Specializations> listAllSpecializations();
}
