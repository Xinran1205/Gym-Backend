package com.gym.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.dto.FitnessCentreDTO;
import com.gym.entity.FitnessCentre;

import java.util.List;


public interface FitnessCentreService extends IService<FitnessCentre> {
    List<FitnessCentre> listAllCentres();
    FitnessCentre getCentreById(Long id);
    FitnessCentre addCentre(FitnessCentreDTO dto);
    FitnessCentre updateCentre(Long id, FitnessCentreDTO dto);
    void deleteCentre(Long id);
}
