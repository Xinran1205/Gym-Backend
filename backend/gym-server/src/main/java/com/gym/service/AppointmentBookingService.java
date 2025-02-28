package com.gym.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.dto.AppointmentBookingDTO;
import com.gym.dto.AppointmentDecisionDTO;
import com.gym.entity.AppointmentBooking;

import java.util.List;


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

    /**
     * 教练同意预约申请，更新预约状态为 Approved，同时更新对应可用时间状态为 Booked，并通知学员
     *
     * @param dto       包含预约ID和可选反馈信息
     * @param trainerId 当前教练ID
     */
    void acceptAppointment(AppointmentDecisionDTO dto, Long trainerId);

    /**
     * 教练拒绝预约申请，更新预约状态为 Rejected，并通知学员
     *
     * @param dto       包含预约ID和可选反馈信息
     * @param trainerId 当前教练ID
     */
    void rejectAppointment(AppointmentDecisionDTO dto, Long trainerId);

    List<AppointmentBooking> getPendingAppointmentsForTrainer(Long trainerId);
}

