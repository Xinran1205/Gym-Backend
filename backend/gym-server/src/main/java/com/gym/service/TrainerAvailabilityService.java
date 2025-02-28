package com.gym.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.dto.AvailabilitySlotDTO;
import com.gym.entity.TrainerAvailability;

import java.util.List;

public interface TrainerAvailabilityService extends IService<TrainerAvailability> {
    /**
     * 更新指定教练的可用时间。该方法将删除该教练所有现有的可用时间（或未来的可用时间），
     * 然后批量插入前端传入的新时间段。
     *
     * @param trainerId         教练的用户ID
     * @param availabilitySlots 前端传入的可用时间段列表
     */
    void updateAvailability(Long trainerId, List<AvailabilitySlotDTO> availabilitySlots);

    /**
     * 查询指定教练从当前时间开始的所有时间段（包括 Available, Booked, Unavailable）
     *
     * @param trainerId 教练的用户ID
     * @return 可用时间段列表
     */
    List<TrainerAvailability> getFutureAvailability(Long trainerId);


    /**
     * 查询指定教练从当前时间开始的所有可用时间段（状态为 Available）
     *
     * @param trainerId 教练的用户ID
     * @return 可用时间段列表
     */
    List<AvailabilitySlotDTO> getAvailableSlots(Long trainerId);
}

