package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.dao.TrainerAvailabilityDao;
import com.gym.dto.AvailabilitySlotDTO;
import com.gym.entity.TrainerAvailability;
import com.gym.service.TrainerAvailabilityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TrainerAvailabilityServiceImpl extends ServiceImpl<TrainerAvailabilityDao, TrainerAvailability>
        implements TrainerAvailabilityService {

    @Override
    @Transactional
    public void updateAvailability(Long tutorId, List<AvailabilitySlotDTO> newSlots) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime limit = now.plusDays(7);

        // 1. 过滤并校验前端数据
        List<AvailabilitySlotDTO> valid = newSlots.stream()
                .filter(s -> !s.getStartTime().isBefore(now)
                        && !s.getEndTime().isAfter(limit)
                        && s.getEndTime().isAfter(s.getStartTime()))
                .collect(Collectors.toList());

        // 2. 查询数据库内已有时段
        List<TrainerAvailability> existing = lambdaQuery()
                .eq(TrainerAvailability::getTrainerId, tutorId)
                .ge(TrainerAvailability::getStartTime, now)
                .le(TrainerAvailability::getEndTime, limit)
                .list();

        // 3. 用 start_end 作为 key 建 map
        Map<String, TrainerAvailability> existMap = existing.stream()
                .collect(Collectors.toMap(
                        v -> v.getStartTime() + "_" + v.getEndTime(),
                        v -> v
                ));

        // 4. 新增：对每个前端传来的时段，如果在已有记录中不存在，则新增
        List<TrainerAvailability> toInsert = valid.stream()
                .filter(v -> !existMap.containsKey(v.getStartTime() + "_" + v.getEndTime()))
                .map(v -> TrainerAvailability.builder()
                        .trainerId(tutorId)
                        .startTime(v.getStartTime())
                        .endTime(v.getEndTime())
                        .status(TrainerAvailability.AvailabilityStatus.Available)
                        .build())
                .collect(Collectors.toList());
        if (!toInsert.isEmpty()) {
            this.saveBatch(toInsert);
        }

        // 5. 删除：对数据库中存在但前端未传的时段，且状态是 Available（未被预约）的，删除它们
        Set<String> newKeys = valid.stream()
                .map(v -> v.getStartTime() + "_" + v.getEndTime())
                .collect(Collectors.toSet());

        List<Long> toDelete = existing.stream()
                .filter(v -> {
                    String key = v.getStartTime() + "_" + v.getEndTime();
                    return !newKeys.contains(key)
                            && v.getStatus() == TrainerAvailability.AvailabilityStatus.Available;
                })
                .map(TrainerAvailability::getAvailabilityId)
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            this.removeByIds(toDelete);
        }
    }



    // 查出教练的所有时间段，包括booked和unavailable（暂时没有unavailable）
    @Override
    public List<TrainerAvailability> getFutureAvailability(Long trainerId) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<TrainerAvailability> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrainerAvailability::getTrainerId, trainerId)
                .ge(TrainerAvailability::getStartTime, now)
                .orderByAsc(TrainerAvailability::getStartTime);
        return this.list(queryWrapper);
    }

    @Override
    public List<AvailabilitySlotDTO> getAvailableSlots(Long trainerId) {
        // 计算缓冲时间：当前时间加1小时
        LocalDateTime cutoff = LocalDateTime.now().plusHours(1);
        LambdaQueryWrapper<TrainerAvailability> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrainerAvailability::getTrainerId, trainerId)
                .eq(TrainerAvailability::getStatus, TrainerAvailability.AvailabilityStatus.Available)
                .ge(TrainerAvailability::getStartTime, cutoff)
                .orderByAsc(TrainerAvailability::getStartTime)
                // 只选择 availabilityId、start_time 和 end_time 字段
                .select(TrainerAvailability::getAvailabilityId, TrainerAvailability::getStartTime, TrainerAvailability::getEndTime);

        List<TrainerAvailability> availabilityList = this.list(queryWrapper);

        return availabilityList.stream()
                .map(item -> AvailabilitySlotDTO.builder()
                        .availabilityId(item.getAvailabilityId())
                        .startTime(item.getStartTime())
                        .endTime(item.getEndTime())
                        .build())
                .collect(Collectors.toList());
    }


}

