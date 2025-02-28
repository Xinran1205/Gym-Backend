package com.gym.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.dto.AppointmentBookingDTO;
import com.gym.entity.AppointmentBooking;


public interface AppointmentBookingService extends IService<AppointmentBooking> {
    /**
     * 学员预约课程（session）业务：
     * 校验学员与教练的连接关系、校验所选可用时间有效，
     * 更新该时间段状态为 Booked、创建预约记录（状态为 Pending），并通知教练审核。
     *
     * @param dto      预约请求数据
     * @param memberId 当前学员的ID
     */
    void bookSession(AppointmentBookingDTO dto, Long memberId);
}

