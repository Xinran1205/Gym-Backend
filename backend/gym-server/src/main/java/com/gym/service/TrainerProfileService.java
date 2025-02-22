package com.gym.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.entity.TrainerProfile;
import com.gym.entity.User;
import com.gym.vo.TrainerAllProfile;

import java.util.List;

public interface TrainerProfileService extends IService<TrainerProfile> {
    public void createDefaultTrainerProfile(Long userId);

    public TrainerAllProfile getTrainerAllProfile(Long userId);
}
