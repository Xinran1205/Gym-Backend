package com.gym.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.entity.TrainingHistory;
import com.gym.entity.TrainingRequest;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrainingHistoryDao extends BaseMapper<TrainingHistory> {
}
