package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.dao.AppointmentBookingDao;
import com.gym.dao.TrainerAvailabilityDao;
import com.gym.dto.AvailabilitySlotDTO;
import com.gym.entity.AppointmentBooking;
import com.gym.entity.TrainerAvailability;
import com.gym.service.TrainerAvailabilityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TrainerAvailabilityServiceImpl extends ServiceImpl<TrainerAvailabilityDao, TrainerAvailability>
        implements TrainerAvailabilityService {

    /** 老师一次最多维护未来 7 天 */
    private static final int RANGE_DAYS = 7;

    @Override
    @Transactional
    public void updateAvailability(Long trainerId, List<AvailabilitySlotDTO> frontList) {

        LocalDateTime   now   = LocalDateTime.now();
        LocalDateTime   limit = now.plusDays(RANGE_DAYS);

        /* ---------- 1. 过滤非法时间段 ---------- */
        List<AvailabilitySlotDTO> valid = frontList.stream()
                .filter(s -> s.getStartTime() != null && s.getEndTime() != null)
                .filter(s -> !s.getStartTime().isBefore(now)           // 必须 ≥ 当前
                        && !s.getEndTime().isAfter(limit)            // 必须 ≤ 7 天后
                        &&  s.getEndTime().isAfter(s.getStartTime()))// 结束 > 开始
                .collect(Collectors.toList());

        if (valid.isEmpty()) {
            log.info("Trainer[{}] availability update skipped – no valid slot.", trainerId);
            return;
        }

        /* ---------- 2. 查询当前 7 天内已存在的 slot ---------- */
        List<TrainerAvailability> dbSlots = lambdaQuery()
                .eq(TrainerAvailability::getTrainerId, trainerId)
                .ge(TrainerAvailability::getStartTime, now)
                .le(TrainerAvailability::getEndTime,   limit)
                .list();

        // 用 “start_end” 当唯一键，避免重复插入
        Set<String> dbKeys = dbSlots.stream()
                .map(v -> v.getStartTime() + "_" + v.getEndTime())
                .collect(Collectors.toSet());

        /* ---------- 3. 只插入新增的 ---------- */
        List<TrainerAvailability> toInsert = valid.stream()
                .filter(s -> s.getAvailabilityId() == null)                // 前端标记为新增
                .filter(s -> !dbKeys.contains(s.getStartTime() + "_" + s.getEndTime())) // 数据库尚无同段
                .map(s -> TrainerAvailability.builder()
                        .trainerId(trainerId)
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .status(TrainerAvailability.AvailabilityStatus.Available)
                        .build())
                .collect(Collectors.toList());

        if (!toInsert.isEmpty()) {
            saveBatch(toInsert);
        }

        log.info("Trainer[{}] availability add-only ⇒ inserted {}", trainerId, toInsert.size());
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

