package com.gym.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.entity.TrainingRequest;
import com.gym.entity.TrainingSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrainingRequestDao extends BaseMapper<TrainingRequest> {
}
