package com.gym.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.dao.FitnessCentreDao;
import com.gym.dto.FitnessCentreDTO;
import com.gym.entity.FitnessCentre;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.service.FitnessCentreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FitnessCentreServiceImpl
        extends ServiceImpl<FitnessCentreDao, FitnessCentre>
        implements FitnessCentreService {

    @Override
    public List<FitnessCentre> listAllCentres() {
        return this.list();
    }

    @Override
    public FitnessCentre getCentreById(Long id) {
        FitnessCentre centre = this.getById(id);
        if (centre == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Fitness centre not found.");
        }
        return centre;
    }

    @Override
    @Transactional
    public FitnessCentre addCentre(FitnessCentreDTO dto) {
        FitnessCentre centre = FitnessCentre.builder()
                .name(dto.getName().trim())
                .address(dto.getAddress().trim())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .contactInfo(dto.getContactInfo().trim())
                .description(dto.getDescription() == null ? null : dto.getDescription().trim())
                .build();
        boolean ok = this.save(centre);
        if (!ok) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Failed to create fitness centre.");
        }
        return centre;
    }

    @Override
    @Transactional
    public FitnessCentre updateCentre(Long id, FitnessCentreDTO dto) {
        FitnessCentre centre = this.getById(id);
        if (centre == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Fitness centre not found.");
        }
        centre.setName(dto.getName().trim());
        centre.setAddress(dto.getAddress().trim());
        centre.setLatitude(dto.getLatitude());
        centre.setLongitude(dto.getLongitude());
        centre.setContactInfo(dto.getContactInfo().trim());
        centre.setDescription(dto.getDescription() == null ? null : dto.getDescription().trim());
        boolean ok = this.updateById(centre);
        if (!ok) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Failed to update fitness centre.");
        }
        return centre;
    }

    @Override
    @Transactional
    public void deleteCentre(Long id) {
        boolean removed = this.removeById(id);
        if (!removed) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Fitness centre not found or already deleted.");
        }
    }
}

