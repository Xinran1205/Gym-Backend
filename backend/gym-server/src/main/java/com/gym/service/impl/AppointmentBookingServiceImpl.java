package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.dao.AppointmentAlternativeTrainerDao;
import com.gym.dao.AppointmentBookingDao;
import com.gym.dto.AppointmentBookingDTO;
import com.gym.dto.AppointmentDecisionDTO;
import com.gym.dto.AppointmentDecisionRejectDTO;
import com.gym.entity.*;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.service.*;
import com.gym.vo.*;
import lombok.extern.slf4j.Slf4j;
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.*;

@Service
@Slf4j
public class AppointmentBookingServiceImpl extends ServiceImpl<AppointmentBookingDao, AppointmentBooking>
        implements AppointmentBookingService {

    @Autowired
    private TrainerConnectRequestService trainerConnectRequestService;

    @Autowired
    private TrainerAvailabilityService trainerAvailabilityService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AppointmentBookingDao appointmentBookingDao;

    @Autowired
    private AppointmentAlternativeTrainerDao appointmentAlternativeTrainerDao;

    @Autowired
    private UserService userService;   // 需要获取学员姓名

    @Autowired
    private WorkoutPlanService workoutPlanService; // 需要获取学员的健身计划

    // 新增：注入 RedissonClient，用于分布式锁
//    @Autowired
//    private RedissonClient redissonClient;

//    @Override
//    @Transactional
//    public void bookSession(AppointmentBookingDTO dto, Long memberId) {
//        // 1. 校验学员与教练之间是否已连接
//        LambdaQueryWrapper<TrainerConnectRequest> connectWrapper = new LambdaQueryWrapper<>();
//        connectWrapper.eq(TrainerConnectRequest::getMemberId, memberId)
//                .eq(TrainerConnectRequest::getTrainerId, dto.getTrainerId())
//                .eq(TrainerConnectRequest::getStatus, TrainerConnectRequest.RequestStatus.Accepted);
//        TrainerConnectRequest connection = trainerConnectRequestService.getOne(connectWrapper);
//        if (connection == null) {
//            throw new CustomException(ErrorCode.FORBIDDEN, "You are not connected with this trainer.");
//        }
//
//        // 2. 校验所选时间段是否存在且有效
//        TrainerAvailability availability = trainerAvailabilityService.getById(dto.getAvailabilityId());
//        if (availability == null) {
//            throw new CustomException(ErrorCode.NOT_FOUND, "Selected time slot not found.");
//        }
//        if (!availability.getTrainerId().equals(dto.getTrainerId())) {
//            throw new CustomException(ErrorCode.BAD_REQUEST, "The selected time slot does not belong to the specified trainer.");
//        }
//        if (!availability.getStatus().equals(TrainerAvailability.AvailabilityStatus.Available)) {
//            throw new CustomException(ErrorCode.BAD_REQUEST, "The selected time slot is not available.");
//        }
//        if (availability.getStartTime().isBefore(LocalDateTime.now().plusHours(1))) {
//            throw new CustomException(ErrorCode.BAD_REQUEST, "The selected time slot is too soon; please select a time at least one hour from now.");
//        }
//
//        // 3. 校验当前用户是否已有冲突预约
//        LocalDateTime newStartTime = availability.getStartTime();
//        LocalDateTime newEndTime = availability.getEndTime();
//        int conflictCount = appointmentBookingDao.countOverlappingAppointments(memberId, newStartTime, newEndTime);
//        if (conflictCount > 0) {
//            throw new CustomException(ErrorCode.BAD_REQUEST, "You already have an appointment in this time slot.");
//        }
//
//        // 4. 使用分布式锁锁定该可用时段，确保并发处理安全
//        String lockKey = "appointment:lock:" + dto.getAvailabilityId();
//        RLock lock = redissonClient.getLock(lockKey);
//        lock.lock();
//        try {
//            // 再次查询并检查最新状态
//            TrainerAvailability currentAvailability = trainerAvailabilityService.getById(dto.getAvailabilityId());
//            if (currentAvailability == null || !currentAvailability.getStatus().equals(TrainerAvailability.AvailabilityStatus.Available)) {
//                throw new CustomException(ErrorCode.BAD_REQUEST, "The selected time slot is no longer available.");
//            }
//
//            // 5. 将时段状态更新为 Booked（预约时立即锁定资源）
//            currentAvailability.setStatus(TrainerAvailability.AvailabilityStatus.Booked);
//            trainerAvailabilityService.updateById(currentAvailability);
//
//            // 6. 创建预约记录，初始状态为 Pending
//            AppointmentBooking booking = AppointmentBooking.builder()
//                    .memberId(memberId)
//                    .trainerId(dto.getTrainerId())
//                    .availabilityId(dto.getAvailabilityId())
//                    .projectName(dto.getProjectName())
//                    .description(dto.getDescription())
//                    .appointmentStatus(AppointmentBooking.AppointmentStatus.Pending)
//                    .build();
//            boolean inserted = this.save(booking);
//            if (!inserted) {
//                throw new CustomException(ErrorCode.BAD_REQUEST, "Failed to create appointment booking.");
//            }
//
//            // 7. 发送通知给教练审核预约请求
//            Notification notification = Notification.builder()
//                    .userId(dto.getTrainerId())
//                    .title("New Session Appointment Request")
//                    .message("You have a new session appointment request for project: " + dto.getProjectName())
//                    .type(Notification.NotificationType.INFO)
//                    .isRead(false)
//                    .build();
//            notificationService.sendNotification(notification);
//
//            log.info("Appointment booking created successfully: Appointment id [{}] for member [{}] and trainer [{}]",
//                    booking.getAppointmentId(), memberId, dto.getTrainerId());
//        } finally {
//            lock.unlock();
//        }
//    }


    @Override
    @Transactional
    public void bookSession(AppointmentBookingDTO dto, Long memberId) {
        // 1. 校验学员与该教练之间是否存在已连接（Accepted）的关系
        // 其实这一步不需要，前端那边是灰的
        LambdaQueryWrapper<TrainerConnectRequest> connectWrapper = new LambdaQueryWrapper<>();
        connectWrapper.eq(TrainerConnectRequest::getMemberId, memberId)
                .eq(TrainerConnectRequest::getTrainerId, dto.getTrainerId())
                .eq(TrainerConnectRequest::getStatus, TrainerConnectRequest.RequestStatus.Accepted);
        TrainerConnectRequest connection = trainerConnectRequestService.getOne(connectWrapper);
        if (connection == null) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not connected with this trainer.");
        }

        // 2. 校验所选可用时间是否存在且有效
        TrainerAvailability availability = trainerAvailabilityService.getById(dto.getAvailabilityId());
        if (availability == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Selected time slot not found.");
        }
        if (!availability.getTrainerId().equals(dto.getTrainerId())) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "The selected time slot does not belong to the specified trainer.");
        }
        if (!availability.getStatus().equals(TrainerAvailability.AvailabilityStatus.Available)) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "The selected time slot is no longer available.");
        }
        if (availability.getStartTime().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "The selected time slot is too soon; please select a time at least one hour from now.");
        }

        // 新增步骤：校验当前用户在该时间段内是否已经有 pending 或 approved 状态的预约
        LocalDateTime newStartTime = availability.getStartTime();
        LocalDateTime newEndTime = availability.getEndTime();
        int conflictCount = appointmentBookingDao.countOverlappingAppointments(memberId, newStartTime, newEndTime);
        if (conflictCount > 0) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "You already have an appointment in this time slot.");
        }


        // 3. 更新可用时间状态为 Booked
        // 3. （此处不更新可用时间状态，允许多个人申请）
//        availability.setStatus(TrainerAvailability.AvailabilityStatus.Booked);
//        boolean availUpdate = trainerAvailabilityService.updateById(availability);
//        if (!availUpdate) {
//            throw new CustomException(ErrorCode.BAD_REQUEST, "Failed to update availability status.");
//        }

        // 4. 创建预约记录，初始状态为 Pending
        AppointmentBooking booking = AppointmentBooking.builder()
                .memberId(memberId)
                .trainerId(dto.getTrainerId())
                .availabilityId(dto.getAvailabilityId())
                .projectName(dto.getProjectName())
                .description(dto.getDescription())
                .appointmentStatus(AppointmentBooking.AppointmentStatus.Pending)
                .build();
        boolean inserted = this.save(booking);
        if (!inserted) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Failed to create appointment booking.");
        }

        // 5. 发送通知给教练审核预约请求
        Notification notification = Notification.builder()
                .userId(dto.getTrainerId())  // 通知目标为教练
                .title("New Session Appointment Request")
                .message("You have a new session appointment request for project: " + dto.getProjectName())
                .type(Notification.NotificationType.INFO)
                .isRead(false)
                .build();
        notificationService.sendNotification(notification);

        log.info("Appointment booking created successfully: Appointment id [{}] for member [{}] and trainer [{}]",
                booking.getAppointmentId(), memberId, dto.getTrainerId());
    }

    @Override
    @Transactional
    public void acceptAppointment(AppointmentDecisionDTO dto, Long trainerId) {
        // 1. 查询预约记录
        AppointmentBooking booking = this.getById(dto.getAppointmentId());
        if (booking == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Appointment booking not found.");
        }
        // 2. 校验该预约是否属于当前教练
        if (!booking.getTrainerId().equals(trainerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not authorized to process this appointment.");
        }
        // 3. 仅允许处理状态为 Pending 的预约
        if (booking.getAppointmentStatus() != AppointmentBooking.AppointmentStatus.Pending) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "This appointment has already been processed.");
        }

        checkAndExpireIfNeeded(booking);

        // 4. 更新预约状态为 Approved
        booking.setAppointmentStatus(AppointmentBooking.AppointmentStatus.Approved);
        boolean updated = this.updateById(booking);
        if (!updated) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Failed to update appointment booking.");
        }

        // 不再修改 TrainerAvailability 状态，因为预约时已锁定
//        // 5. 更新对应的可用时间状态为 Booked
//        // 这里太对了！！！
        TrainerAvailability availability = trainerAvailabilityService.getById(booking.getAvailabilityId());
        if (availability != null) {
            availability.setStatus(TrainerAvailability.AvailabilityStatus.Booked);
            trainerAvailabilityService.updateById(availability);
        }
        // 6. 发送通知给学员
        Notification notification = Notification.builder()
                .userId(booking.getMemberId())
                .title("Appointment Approved")
                .message("Your appointment for project '" + booking.getProjectName() + "' has been approved by the trainer." +
                        (dto.getResponseMessage() != null ? " Note: " + dto.getResponseMessage() : ""))
                .type(Notification.NotificationType.ALERT)
                .isRead(false)
                .build();
        notificationService.sendNotification(notification);

        log.info("Trainer [{}] accepted appointment [{}]", trainerId, dto.getAppointmentId());
    }

    @Override
    @Transactional
    public void rejectAppointment(AppointmentDecisionRejectDTO dto, Long trainerId) {
        // 1. 查询预约记录
        AppointmentBooking booking = this.getById(dto.getAppointmentId());
        if (booking == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Appointment booking not found.");
        }
        // 2. 校验该预约是否属于当前教练
        if (!booking.getTrainerId().equals(trainerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not authorized to process this appointment.");
        }
        // 3. 仅允许处理状态为 Pending 的预约
        if (booking.getAppointmentStatus() != AppointmentBooking.AppointmentStatus.Pending) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "This appointment has already been processed.");
        }

        // 校验是否过期
        checkAndExpireIfNeeded(booking);

        // 4. 更新预约状态为 Rejected
        booking.setAppointmentStatus(AppointmentBooking.AppointmentStatus.Rejected);
        // 5. 如果有指定替代教练，则在 appointment_alternative_trainer 表中插入记录
        if (dto.getAlternativeTrainerId() != null) {
            AppointmentAlternativeTrainer alternative = AppointmentAlternativeTrainer.builder()
                    .appointmentId(dto.getAppointmentId())
                    .alternativeTrainerId(dto.getAlternativeTrainerId())
                    .alternativeTrainerName(dto.getAlternativeTrainerName())
                    .build();
            appointmentAlternativeTrainerDao.insert(alternative);
        }

        boolean updated = this.updateById(booking);
        if (!updated) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Failed to update appointment booking.");
        }

        // 更新时段状态为 Available
        TrainerAvailability availability = trainerAvailabilityService.getById(booking.getAvailabilityId());
        if (availability != null) {
            availability.setStatus(TrainerAvailability.AvailabilityStatus.Available);
            trainerAvailabilityService.updateById(availability);
        }

        // 5. 发送通知给学员
        Notification notification = Notification.builder()
                .userId(booking.getMemberId())
                .title("Appointment Rejected")
                .message("Your appointment for project '" + booking.getProjectName() + "' has been rejected by the trainer." +
                        (dto.getResponseMessage() != null ? " Note: " + dto.getResponseMessage() : "") +
                        // 如果没有替代教练，显示None, 这里通知里面显示的是替代教练的名字！
                        " Alternative trainer: " + (dto.getAlternativeTrainerName() == null ? "None" : dto.getAlternativeTrainerName()))
                .type(Notification.NotificationType.INFO)
                .isRead(false)
                .build();
        notificationService.sendNotification(notification);

        log.info("Trainer [{}] rejected appointment [{}]", trainerId, dto.getAppointmentId());
    }

    // 教练查询待审核预约请求接口（仅返回状态为 Pending 且未过期的预约）
    @Override
    public List<PendingAppointmentVO> getPendingAppointmentsForTrainerWithTimes(Long trainerId) {
        // 1. 查询 Pending 预约并做过期检查
        LambdaQueryWrapper<AppointmentBooking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentBooking::getTrainerId, trainerId)
                .eq(AppointmentBooking::getAppointmentStatus, AppointmentBooking.AppointmentStatus.Pending)
                .orderByAsc(AppointmentBooking::getCreateTime);
        List<AppointmentBooking> list = this.list(wrapper);

        // 先筛选未过期的
        List<AppointmentBooking> valid = new ArrayList<>();
        for (AppointmentBooking b : list) {
            try {
                checkAndExpireIfNeeded(b);
                valid.add(b);
            } catch (CustomException e) {
                log.info("Appointment [{}] expired.", b.getAppointmentId());
            }
        }
        // 如果没有有效记录，直接返回空列表
        if (valid.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集所有 memberId，去重并保持顺序
        LinkedHashSet<Long> memberIds = valid.stream()
                .map(AppointmentBooking::getMemberId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 2. 批量拉取学员信息（此时 memberIds 一定非空）
        Map<Long, String> nameMap = userService.listByIds(new ArrayList<>(memberIds))
                .stream()
                .collect(Collectors.toMap(User::getUserID, User::getName));

        // 3. 组装 VO 列表
        List<PendingAppointmentVO> result = new ArrayList<>();
        for (AppointmentBooking b : valid) {
            TrainerAvailability slot = trainerAvailabilityService.getById(b.getAvailabilityId());
            if (slot == null) {
                // 如果时段数据异常，可以跳过
                continue;
            }
            result.add(PendingAppointmentVO.builder()
                    .appointmentId(b.getAppointmentId())
                    .memberId(b.getMemberId())
                    .memberName(nameMap.getOrDefault(b.getMemberId(), "Unknown"))
                    .projectName(b.getProjectName())
                    .description(b.getDescription())
                    .createdAt(b.getCreateTime())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .build());
        }
        return result;
    }

    // 在审批之前，我们统一检查预约记录对应的可用时间是否已经过期
    // 如果 availability.getStartTime() is before now, 则标记 booking 为 Expired，发送通知，并终止处理
    private void checkAndExpireIfNeeded(AppointmentBooking booking) {
        TrainerAvailability availability = trainerAvailabilityService.getById(booking.getAvailabilityId());
        // 如果可用时间不存在，或该时间距离当前时间不足1小时（即在 now + 1 小时之前），则认为已过期
        if (availability == null || availability.getStartTime().isBefore(LocalDateTime.now().plusHours(1))) {
            // 更新预约状态为 Expired
            booking.setAppointmentStatus(AppointmentBooking.AppointmentStatus.Expired);
            this.updateById(booking);
            // 发送通知给学员，告知该预约申请已过期
            Notification notification = Notification.builder()
                    .userId(booking.getMemberId())
                    .title("Appointment Expired")
                    .message("Your appointment request for project '" + booking.getProjectName() + "' has expired because the selected time slot is too close to the current time.")
                    .type(Notification.NotificationType.INFO)
                    .isRead(false)
                    .build();
            notificationService.sendNotification(notification);
            throw new CustomException(ErrorCode.BAD_REQUEST, "This appointment request has expired and cannot be processed.");
        }
    }

    @Override
    public Page<AppointmentBookingDetailVO> getUpcomingAppointmentsForMember(Long memberId, Page<AppointmentBookingDetailVO> page,String status) {
        // 先更新状态：过期和完成的
        expireOldPendingAppointments(memberId);
        updateCompletedAppointments(memberId);
        // 分页查询仅返回状态为 Pending 和 Approved 的记录，并且对应的 TrainerAvailability.start_time > NOW()
        return baseMapper.selectUpcomingAppointmentsByMember(page, memberId,status);
    }


    /**
     * 批量检查并更新当前会员所有 Pending 预约，若对应可用时间不足1小时，则更新状态为 Expired
     *
     * @param memberId 当前会员ID
     */
    private void expireOldPendingAppointments(Long memberId) {
        // 查询当前会员所有 Pending 预约
        LambdaQueryWrapper<AppointmentBooking> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppointmentBooking::getMemberId, memberId)
                .eq(AppointmentBooking::getAppointmentStatus, AppointmentBooking.AppointmentStatus.Pending);
        List<AppointmentBooking> pendingList = this.list(queryWrapper);
        LocalDateTime cutoff = LocalDateTime.now().plusHours(1);
        for (AppointmentBooking booking : pendingList) {
            TrainerAvailability availability = trainerAvailabilityService.getById(booking.getAvailabilityId());
            if (availability == null || availability.getStartTime().isBefore(cutoff)) {
                booking.setAppointmentStatus(AppointmentBooking.AppointmentStatus.Expired);
                this.updateById(booking);
                // 可选：通知会员申请已过期
                Notification notification = Notification.builder()
                        .userId(booking.getMemberId())
                        .title("Appointment Expired")
                        .message("Your appointment request for project '" + booking.getProjectName() + "' has expired due to insufficient lead time.")
                        .type(Notification.NotificationType.INFO)
                        .isRead(false)
                        .build();
                notificationService.sendNotification(notification);
            }
        }
    }

    /**
     * 检查当前会员所有 Approved 预约，如果对应 TrainerAvailability 的 end_time 已经过了当前时间，则更新状态为 Completed
     */
    private void updateCompletedAppointments(Long memberId) {
        LambdaQueryWrapper<AppointmentBooking> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppointmentBooking::getMemberId, memberId)
                .eq(AppointmentBooking::getAppointmentStatus, AppointmentBooking.AppointmentStatus.Approved);
        List<AppointmentBooking> approvedList = this.list(queryWrapper);
        for (AppointmentBooking booking : approvedList) {
            TrainerAvailability availability = trainerAvailabilityService.getById(booking.getAvailabilityId());
            if (availability != null && availability.getEndTime().isBefore(LocalDateTime.now())) {
                booking.setAppointmentStatus(AppointmentBooking.AppointmentStatus.Completed);
                this.updateById(booking);
                // 可选：通知会员课程已完成
                Notification notification = Notification.builder()
                        .userId(booking.getMemberId())
                        .title("Appointment Completed")
                        .message("Your appointment for project '" + booking.getProjectName() + "' has been completed.")
                        .type(Notification.NotificationType.INFO)
                        .isRead(false)
                        .build();
                notificationService.sendNotification(notification);
            }
        }
    }

    @Override
    public Page<AppointmentBookingHistoryDetailVO> getHistoricalAppointmentsForMember(Long memberId, Page<AppointmentBookingHistoryDetailVO> page, String status) {
        // 查询历史记录：状态不是 Pending 和 Approved（包括 Expired、Rejected、Cancelled、Completed）
        return baseMapper.selectHistoricalAppointmentsByMember(page, memberId,status);
    }


    @Override
    @Transactional
    public void cancelAppointment(Long appointmentId, Long memberId) {
        // 1. 查询预约记录
        AppointmentBooking booking = this.getById(appointmentId);
        if (booking == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Appointment booking not found.");
        }
        // 2. 校验预约是否属于当前会员
        if (!booking.getMemberId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not authorized to cancel this appointment.");
        }
        // 3. 判断预约状态
        if (booking.getAppointmentStatus() == AppointmentBooking.AppointmentStatus.Pending) {
            // 对于 Pending 状态，允许直接取消
            booking.setAppointmentStatus(AppointmentBooking.AppointmentStatus.Cancelled);
            boolean updated = this.updateById(booking);
            if (!updated) {
                throw new CustomException(ErrorCode.BAD_REQUEST, "Failed to cancel the appointment.");
            }
            // 更新可用时段状态为 Available（释放预约时锁定的时段）
            TrainerAvailability availability = trainerAvailabilityService.getById(booking.getAvailabilityId());
            if (availability != null) {
                availability.setStatus(TrainerAvailability.AvailabilityStatus.Available);
                trainerAvailabilityService.updateById(availability);
            }

            // 发送通知给教练，告知该预约已被取消
            Notification notification = Notification.builder()
                    .userId(booking.getTrainerId())
                    .title("Appointment Cancelled")
                    .message("The appointment request for project '" + booking.getProjectName() + "' has been cancelled by the member.")
                    .type(Notification.NotificationType.INFO)
                    .isRead(false)
                    .build();
            notificationService.sendNotification(notification);
            log.info("Member [{}] cancelled appointment [{}]", memberId, appointmentId);
        } else if (booking.getAppointmentStatus() == AppointmentBooking.AppointmentStatus.Approved) {
            // 对于已 Approved 的预约，不允许直接取消，提示用户通过私下沟通取消
            throw new CustomException(ErrorCode.BAD_REQUEST, "Approved appointments cannot be cancelled directly. Please contact your trainer.");
        } else {
            // 其他状态（如 Expired、Cancelled、Completed等）不允许取消
            throw new CustomException(ErrorCode.BAD_REQUEST, "This appointment cannot be cancelled.");
        }
    }

    @Override
    public DynamicAppointmentStatisticsVO getDynamicAppointmentStatisticsForMember(Long memberId, LocalDate startDate, LocalDate endDate) {
        // 校验日期范围：确保 startDate <= endDate 且不超过30天
        if (startDate.isAfter(endDate)) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Start date must be before or equal to end date.");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > 30) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Date range should not exceed 30 days.");
        }

        // 调用 DAO 层查询每日统计数据
        List<DailyStatisticVO> dailyStats =
                appointmentBookingDao.selectDynamicStatisticsByMember(memberId, startDate, endDate);

//        比如，如果会员在 2 月 1 日完成了 2 小时的课程，2 月 3 日完成了 1 小时的课程，那么 DAO 可能返回的列表是：
//         [DailyStatisticVO(date=2025-02-01, hours=2), DailyStatisticVO(date=2025-02-03, hours=1)]

//        补全日期数据：
//        前端希望看到完整的日期序列，也就是说，在指定日期范围内的每一天都应该有一个统计数据，即使有的天没有数据，也显示 0。
//        代码通过一个 for 循环，从 startDate 到 endDate（包含头尾日期），依次检查：
//        对于每个日期，使用 lambda 表达式在 DAO 返回的 dailyStats 列表中查找是否有对应的记录（即该日期的数据）。
//        如果找到了，就使用该记录；如果没有找到，则创建一个新的 DailyStatisticVO 对象，设置该日期和 0 小时。
//        最后，构造出一个完整的列表 completeStats，保证整个日期范围内每天都有统计数据。
        List<DailyStatisticVO> completeStats = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date; // 确保在 lambda 中使用 final 变量
            DailyStatisticVO stat = dailyStats.stream()
                    .filter(ds -> ds.getDate().equals(currentDate))
                    .findFirst()
                    .orElse(DailyStatisticVO.builder().date(currentDate).hours(0.0).build());
            completeStats.add(stat);
        }

        return DynamicAppointmentStatisticsVO.builder()
                .dailyStatistics(completeStats)
                .build();
    }



    @Override
    public List<PendingAppointmentVO> getApprovedAppointmentsForTrainerWithTimes(Long trainerId) {
        // 1. 查询所有 Approved 预约
        List<AppointmentBooking> list = this.list(new LambdaQueryWrapper<AppointmentBooking>()
                .eq(AppointmentBooking::getTrainerId, trainerId)
                .eq(AppointmentBooking::getAppointmentStatus, AppointmentBooking.AppointmentStatus.Approved)
                .orderByAsc(AppointmentBooking::getCreateTime)
        );
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 批量拉取 memberName
        LinkedHashSet<Long> memberIds = list.stream()
                .map(AppointmentBooking::getMemberId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> nameMap = userService.listByIds(new ArrayList<>(memberIds))
                .stream()
                .collect(Collectors.toMap(User::getUserID, User::getName));

        // 3. 批量拉取 WorkoutPlan（可为空）
        Set<Long> planIds = list.stream()
                .map(AppointmentBooking::getWorkoutPlanId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, WorkoutPlan> planMap = planIds.isEmpty()
                ? Collections.emptyMap()
                : workoutPlanService.listByIds(new ArrayList<>(planIds))
                .stream().collect(Collectors.toMap(WorkoutPlan::getPlanId, p -> p));

        // 4. 组装 VO
        List<PendingAppointmentVO> result = new ArrayList<>();
        for (AppointmentBooking b : list) {
            TrainerAvailability slot = trainerAvailabilityService.getById(b.getAvailabilityId());
            if (slot == null) continue;

            WorkoutPlan plan = (b.getWorkoutPlanId() != null ? planMap.get(b.getWorkoutPlanId()) : null);

            result.add(PendingAppointmentVO.builder()
                    .appointmentId(b.getAppointmentId())
                    .memberId(b.getMemberId())
                    .memberName(nameMap.getOrDefault(b.getMemberId(), "Unknown"))
                    .projectName(b.getProjectName())
                    .description(b.getDescription())
                    .createdAt(b.getCreateTime())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    // workoutPlanId
                    .workoutPlanId(b.getWorkoutPlanId())
                    .workoutPlanTitle(plan != null ? plan.getTitle() : null)
                    .workoutPlanContent(plan != null ? plan.getContent() : null)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional
    public void completeAppointment(Long appointmentId, Long trainerId) {
        // 1. 基本合法性校验
        AppointmentBooking booking = this.getById(appointmentId);
        if (booking == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Appointment booking not found.");
        }
        if (!booking.getTrainerId().equals(trainerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not authorized to complete this appointment.");
        }
        if (booking.getAppointmentStatus() != AppointmentBooking.AppointmentStatus.Approved) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Only approved appointments can be marked as completed.");
        }

        // 2. 更新预约状态 → Completed
        booking.setAppointmentStatus(AppointmentBooking.AppointmentStatus.Completed);
        if (!this.updateById(booking)) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Failed to update appointment booking.");
        }

        // 没必要
//        // 3. 释放时段（把 Booked 设回 Available，方便后续复用/统计）
//        TrainerAvailability availability = trainerAvailabilityService.getById(booking.getAvailabilityId());
//        if (availability != null && availability.getStatus() == TrainerAvailability.AvailabilityStatus.Booked) {
//            availability.setStatus(TrainerAvailability.AvailabilityStatus.Available);
//            trainerAvailabilityService.updateById(availability);
//        }

        // 没必要
        // 4. 通知学员
//        Notification notification = Notification.builder()
//                .userId(booking.getMemberId())
//                .title("Session Completed")
//                .message("Your appointment for project '" + booking.getProjectName() +
//                        "' has been marked as completed by the trainer.")
//                .type(Notification.NotificationType.INFO)
//                .isRead(false)
//                .build();
//        notificationService.sendNotification(notification);

        log.info("Trainer [{}] completed appointment [{}]", trainerId, appointmentId);
    }

    @Override
    public List<MemberAppointmentsVO> getAllAppointmentsGroupedByMember(Long trainerId) {
        // 1. 拉取该教练所有预约，按 memberId、createdAt 排序
        LambdaQueryWrapper<AppointmentBooking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentBooking::getTrainerId, trainerId)
                .orderByAsc(AppointmentBooking::getMemberId)
                .orderByAsc(AppointmentBooking::getCreateTime);
        List<AppointmentBooking> all = this.list(wrapper);
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 按 memberId 分组（LinkedHashMap 保持插入顺序）
        Map<Long, List<AppointmentBooking>> grouped = all.stream()
                .collect(Collectors.groupingBy(
                        AppointmentBooking::getMemberId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 3. 批量拉取所有 member 的姓名
        Set<Long> memberIds = grouped.keySet();
        Map<Long, String> nameMap = userService.listByIds(new ArrayList<>(memberIds))
                .stream()
                .collect(Collectors.toMap(User::getUserID, User::getName));

        // 4. 构造 VO 列表
        List<MemberAppointmentsVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<AppointmentBooking>> entry : grouped.entrySet()) {
            Long memberId = entry.getKey();
            String memberName = nameMap.getOrDefault(memberId, "Unknown");

            // 把每个 AppointmentBooking 转换成 AppointmentWithTimeVO
            List<AppointmentWithTimeVO> voList = entry.getValue().stream().map(b -> {
                // 查对应的时段
                TrainerAvailability slot = trainerAvailabilityService.getById(b.getAvailabilityId());
                if (slot == null) {
                    throw new CustomException(
                            ErrorCode.BAD_REQUEST,
                            "TrainerAvailability not found for id " + b.getAvailabilityId()
                    );
                }
                return AppointmentWithTimeVO.builder()
                        .appointmentId(b.getAppointmentId())
                        .projectName(b.getProjectName())
                        .description(b.getDescription())
                        .appointmentStatus(b.getAppointmentStatus().name())
                        .createdAt(b.getCreateTime())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .build();
            }).collect(Collectors.toList());

            result.add(MemberAppointmentsVO.builder()
                    .memberId(memberId)
                    .memberName(memberName)
                    .appointments(voList)
                    .build()
            );
        }
        return result;
    }

    @Override
    public List<CompletedAppointmentVO> getCompletedAppointmentsForTrainer(Long trainerId) {
        // 1. 查询所有 Completed 的预约
        LambdaQueryWrapper<AppointmentBooking> qw = new LambdaQueryWrapper<>();
        qw.eq(AppointmentBooking::getTrainerId, trainerId)
                .eq(AppointmentBooking::getAppointmentStatus, AppointmentBooking.AppointmentStatus.Completed)
                .orderByAsc(AppointmentBooking::getCreateTime);
        List<AppointmentBooking> list = this.list(qw);
        if (list.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 批量取学员信息
        Set<Long> memberIds = list.stream()
                .map(AppointmentBooking::getMemberId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> nameMap = userService.listByIds(new ArrayList<>(memberIds)).stream()
                .collect(Collectors.toMap(User::getUserID, User::getName));

        // 3. 构造 VO 列表
        List<CompletedAppointmentVO> result = new ArrayList<>();
        for (AppointmentBooking ab : list) {
            // 3.1 拿到对应的时间段
            TrainerAvailability slot = trainerAvailabilityService.getById(ab.getAvailabilityId());
            if (slot == null) {
                continue; // 若丢失数据则跳过
            }
            result.add(CompletedAppointmentVO.builder()
                    .appointmentId(ab.getAppointmentId())
                    .memberId(ab.getMemberId())
                    .memberName(nameMap.getOrDefault(ab.getMemberId(), "Unknown"))
                    .projectName(ab.getProjectName())
                    .description(ab.getDescription())
                    .createdAt(ab.getCreateTime())
                    .startTime(slot.getStartTime())  // 填充开始时间
                    .endTime(slot.getEndTime())      // 填充结束时间
                    .build());
        }
        return result;
    }

    @Override
    public DynamicAppointmentStatisticsVO getDynamicAppointmentStatisticsForTrainer(
            Long trainerId, LocalDate startDate, LocalDate endDate) {

        // ---- 1. 参数校验 ----
        if (startDate.isAfter(endDate)) {
            throw new CustomException(ErrorCode.BAD_REQUEST,
                    "Start date must be before or equal to end date.");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > 370) {
            throw new CustomException(ErrorCode.BAD_REQUEST,
                    "Date range should not exceed 370 days.");
        }

        // ---- 2. DAO 查询 ----
        List<DailyStatisticVO> stats =
                appointmentBookingDao.selectDynamicStatisticsByTrainer(
                        trainerId, startDate, endDate);

        // ---- 3. 补全缺失日期 ----
        List<DailyStatisticVO> complete = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            final LocalDate cd = d;
            DailyStatisticVO dayStat = stats.stream()
                    .filter(s -> s.getDate().equals(cd))
                    .findFirst()
                    .orElse(DailyStatisticVO.builder()
                            .date(cd)
                            .hours(0.0)
                            .build());
            complete.add(dayStat);
        }

        return DynamicAppointmentStatisticsVO.builder()
                .dailyStatistics(complete)
                .build();
    }

    @Override
    @Transactional
    public void bindWorkoutPlan(Long trainerId, Long appointmentId, Long planId) {
        AppointmentBooking booking = getById(appointmentId);
        if (booking == null || !booking.getTrainerId().equals(trainerId))
            throw new CustomException(ErrorCode.FORBIDDEN, "No permission");

        WorkoutPlan plan = workoutPlanService.getById(planId);
        if (plan == null || !plan.getTrainerId().equals(trainerId))
            throw new CustomException(ErrorCode.BAD_REQUEST, "Plan not found or not yours");

        booking.setWorkoutPlanId(planId);
        updateById(booking);
    }
}

