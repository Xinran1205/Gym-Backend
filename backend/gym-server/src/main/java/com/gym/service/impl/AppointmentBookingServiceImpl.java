package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.dao.AppointmentBookingDao;
import com.gym.dto.AppointmentBookingDTO;
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
}

