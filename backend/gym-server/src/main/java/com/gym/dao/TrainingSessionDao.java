package com.gym.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.entity.TrainingSession;
import com.gym.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrainingSessionDao extends BaseMapper<TrainingSession> {
}
