package com.gym.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.dto.WorkoutPlanDTO;
import com.gym.entity.WorkoutPlan;

import java.util.List;

public interface WorkoutPlanService extends IService<WorkoutPlan> {

    WorkoutPlan createPlan(Long trainerId, WorkoutPlanDTO dto);

    WorkoutPlan updatePlan(Long trainerId, Long planId, WorkoutPlanDTO dto);

    void         deletePlan(Long trainerId, Long planId);

    List<WorkoutPlan> listPlans(Long trainerId);
}
