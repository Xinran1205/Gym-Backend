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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TrainerAvailabilityServiceImpl extends ServiceImpl<TrainerAvailabilityDao, TrainerAvailability>
        implements TrainerAvailabilityService {

    @Override
    @Transactional
    public void updateAvailability(Long trainerId, List<AvailabilitySlotDTO> newSlots) {
        // 1. 获取当前时间和一周后时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekLater = now.plusWeeks(1);

        // 2. 过滤前端传来的时间段，确保在规定范围内（也可以在前端限制，这里再做一次校验）
        List<AvailabilitySlotDTO> filteredNewSlots = newSlots.stream()
                .filter(slot -> !slot.getStartTime().isBefore(now) && !slot.getEndTime().isAfter(oneWeekLater))
                .collect(Collectors.toList());

        // 3. 查询当前教练在 [now, oneWeekLater] 内已有的可用时间记录
        LambdaQueryWrapper<TrainerAvailability> query = new LambdaQueryWrapper<>();
        query.eq(TrainerAvailability::getTrainerId, trainerId)
                .ge(TrainerAvailability::getStartTime, now)
                .le(TrainerAvailability::getEndTime, oneWeekLater);
        List<TrainerAvailability> existingAvailabilities = this.list(query);

        // 4. 构建一个以“开始时间_结束时间”为 key 的 Map（注意：格式可根据实际情况调整）
        Map<String, TrainerAvailability> existingMap = existingAvailabilities.stream()
                .collect(Collectors.toMap(
                        avail -> avail.getStartTime().toString() + "_" + avail.getEndTime().toString(),
                        avail -> avail
                ));

        // 5. 构建前端传来的可用时间的 key 集合
        Set<String> newSlotKeys = filteredNewSlots.stream()
                .map(slot -> slot.getStartTime().toString() + "_" + slot.getEndTime().toString())
                .collect(Collectors.toSet());

        // 6. 新增：对每个前端传来的时间段，如果在已有记录中不存在，则新增
        List<TrainerAvailability> toInsert = new ArrayList<>();
        for (AvailabilitySlotDTO slot : filteredNewSlots) {
            String key = slot.getStartTime().toString() + "_" + slot.getEndTime().toString();
            if (!existingMap.containsKey(key)) {
                TrainerAvailability avail = TrainerAvailability.builder()
                        .trainerId(trainerId)
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .status(TrainerAvailability.AvailabilityStatus.Available)
                        .build();
                toInsert.add(avail);
            }
        }
        if (!toInsert.isEmpty()) {
            this.saveBatch(toInsert);
        }

        // 7. 删除：对于数据库中存在但前端未传的时间段，删除它们
        List<Long> toDeleteIds = existingAvailabilities.stream()
                .filter(avail -> {
                    String key = avail.getStartTime().toString() + "_" + avail.getEndTime().toString();
                    // 这里可以增加判断：如果该时间段已被预约(Booked)则不允许删除（业务逻辑扩展）
                    return !newSlotKeys.contains(key);
                })
                .map(TrainerAvailability::getAvailabilityId)
                .collect(Collectors.toList());
        if (!toDeleteIds.isEmpty()) {
            this.removeByIds(toDeleteIds);
        }

        log.info("Trainer [{}] availability updated: {} new slots inserted, {} slots deleted",
                trainerId, toInsert.size(), toDeleteIds.size());
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

