package com.gym.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.gym.entity.WorkoutPlan;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkoutPlanDao extends BaseMapper<WorkoutPlan> {
}
