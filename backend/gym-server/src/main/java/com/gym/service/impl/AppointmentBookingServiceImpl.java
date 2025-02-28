package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.dao.AppointmentBookingDao;
import com.gym.dto.AppointmentBookingDTO;
import com.gym.dto.AppointmentDecisionDTO;
import com.gym.entity.AppointmentBooking;
import com.gym.entity.Notification;
import com.gym.entity.TrainerAvailability;
import com.gym.entity.TrainerConnectRequest;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.service.AppointmentBookingService;
import com.gym.service.NotificationService;
import com.gym.service.TrainerAvailabilityService;
import com.gym.service.TrainerConnectRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        if (availability.getStartTime().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "The selected time slot has already passed.");
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
        // 5. 更新对应的可用时间状态为 Booked
        // 这里太对了！！！
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
    public void rejectAppointment(AppointmentDecisionDTO dto, Long trainerId) {
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
        boolean updated = this.updateById(booking);
        if (!updated) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Failed to update appointment booking.");
        }
        // 5. 发送通知给学员
        Notification notification = Notification.builder()
                .userId(booking.getMemberId())
                .title("Appointment Rejected")
                .message("Your appointment for project '" + booking.getProjectName() + "' has been rejected by the trainer." +
                        (dto.getResponseMessage() != null ? " Note: " + dto.getResponseMessage() : ""))
                .type(Notification.NotificationType.INFO)
                .isRead(false)
                .build();
        notificationService.sendNotification(notification);

        log.info("Trainer [{}] rejected appointment [{}]", trainerId, dto.getAppointmentId());
    }

    // 教练查询待审核预约请求接口（仅返回状态为 Pending 且未过期的预约）
    @Override
    public List<AppointmentBooking> getPendingAppointmentsForTrainer(Long trainerId) {
        LambdaQueryWrapper<AppointmentBooking> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppointmentBooking::getTrainerId, trainerId)
                .eq(AppointmentBooking::getAppointmentStatus, AppointmentBooking.AppointmentStatus.Pending)
                .orderByAsc(AppointmentBooking::getCreatedAt);
        List<AppointmentBooking> list = this.list(queryWrapper);
        // 逐条检查并更新已过期的记录
        List<AppointmentBooking> validList = new ArrayList<>();
        for (AppointmentBooking booking : list) {
            try {
                // 该方法内部会检查并抛出异常，如果过期
                checkAndExpireIfNeeded(booking);
                validList.add(booking);
            } catch (CustomException e) {
                // 记录已更新为 Expired，但不加入有效列表
                log.info("Appointment [{}] expired.", booking.getAppointmentId());
            }
        }
        return validList;
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


}

