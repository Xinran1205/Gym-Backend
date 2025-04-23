package com.gym.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.dao.WorkoutPlanDao;
import com.gym.dto.WorkoutPlanDTO;
import com.gym.entity.WorkoutPlan;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.service.WorkoutPlanService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkoutPlanServiceImpl extends ServiceImpl<WorkoutPlanDao, WorkoutPlan>
        implements WorkoutPlanService {

    @Override
    public WorkoutPlan createPlan(Long trainerId, WorkoutPlanDTO dto) {
        WorkoutPlan plan = WorkoutPlan.builder()
                .trainerId(trainerId)
                .title(dto.getTitle().trim())
                .content(dto.getContent())
                .build();
        this.save(plan);
        return plan;
    }

    @Override
    public WorkoutPlan updatePlan(Long trainerId, Long planId, WorkoutPlanDTO dto) {
        WorkoutPlan plan = getById(planId);
        if (plan == null || !plan.getTrainerId().equals(trainerId))
            throw new CustomException(ErrorCode.FORBIDDEN, "Plan not found or no permission");
        plan.setTitle(dto.getTitle().trim());
        plan.setContent(dto.getContent());
        this.updateById(plan);
        return plan;
    }

    @Override
    public void deletePlan(Long trainerId, Long planId) {
        WorkoutPlan plan = getById(planId);
        if (plan == null || !plan.getTrainerId().equals(trainerId))
            throw new CustomException(ErrorCode.FORBIDDEN, "Plan not found or no permission");
        this.removeById(planId);
    }

    @Override
    public List<WorkoutPlan> listPlans(Long trainerId) {
        return lambdaQuery().eq(WorkoutPlan::getTrainerId, trainerId)
                .orderByDesc(WorkoutPlan::getCreateTime)
                .list();
    }
}
