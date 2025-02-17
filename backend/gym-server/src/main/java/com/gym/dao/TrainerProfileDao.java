package com.gym.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.entity.TrainerProfile;
import com.gym.entity.TrainingHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrainerProfileDao extends BaseMapper<TrainerProfile> {
}
