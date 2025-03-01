package com.gym.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.dto.AppointmentBookingDTO;
import com.gym.dto.AppointmentDecisionDTO;
import com.gym.entity.AppointmentBooking;
import com.gym.vo.AppointmentBookingDetailVO;
import com.gym.vo.DynamicAppointmentStatisticsVO;

import java.time.LocalDate;
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

    /**
     * 分页查询当前会员未来预约的详细信息，
     * 先更新 Pending/Approved 状态下已过期或已结束的记录，
     * 返回状态为 Pending 和 Approved 的记录。
     *
     * @param memberId 当前会员ID
     * @param page 分页对象
     * @return 分页预约详情
     */
    Page<AppointmentBookingDetailVO> getUpcomingAppointmentsForMember(Long memberId, Page<AppointmentBookingDetailVO> page);

    /**
     * 分页查询当前会员历史预约记录，
     * 返回状态不是 Pending 和 Approved 的记录。
     *
     * @param memberId 当前会员ID
     * @param page 分页对象
     * @return 分页历史预约详情
     */
    Page<AppointmentBookingDetailVO> getHistoricalAppointmentsForMember(Long memberId, Page<AppointmentBookingDetailVO> page);


    /**
     * 取消预约请求
     * 对于状态为 Pending 的预约，可以直接取消；如果状态为 Approved，则不允许直接取消。
     *
     * @param appointmentId 预约记录ID
     * @param memberId 当前会员的ID
     */
    void cancelAppointment(Long appointmentId, Long memberId);


    /**
     * 查询当前会员在指定日期范围内的预约统计数据，
     * 返回每日完成的课程时数（单位：小时）。
     *
     * @param memberId 当前会员ID
     * @param startDate 统计开始日期（格式 yyyy-MM-dd）
     * @param endDate   统计结束日期（格式 yyyy-MM-dd）
     * @return 动态统计数据 VO
     */
    DynamicAppointmentStatisticsVO getDynamicAppointmentStatisticsForMember
    (Long memberId, LocalDate startDate, LocalDate endDate);
}

