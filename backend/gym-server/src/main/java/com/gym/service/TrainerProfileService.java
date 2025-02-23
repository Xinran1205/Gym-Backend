package com.gym.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.dto.TrainerProfileQuery;
import com.gym.entity.TrainerProfile;
import com.gym.entity.User;
import com.gym.vo.TrainerAllProfile;
import com.gym.vo.TrainerProfileVO;

import java.util.List;

public interface TrainerProfileService extends IService<TrainerProfile> {
    public void createDefaultTrainerProfile(Long userId, String name);

    public TrainerAllProfile getTrainerAllProfile(Long userId);

    public Page<TrainerProfileVO> listTrainers(TrainerProfileQuery query);
}
