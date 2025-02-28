package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.dto.*;
import com.gym.entity.AppointmentBooking;
import com.gym.entity.TrainerAvailability;
import com.gym.entity.TrainerProfile;
import com.gym.entity.User;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.result.RestResult;
import com.gym.service.*;
import com.gym.util.SecurityUtils;
import com.gym.vo.TrainerAllProfile;
import com.gym.vo.TrainerProfileVO;
import com.gym.vo.UserProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/trainer")
@Slf4j
@PreAuthorize("hasRole('trainer')")
public class TrainerController {

    @Autowired
    private TrainerProfileService trainerProfileService;

    @Autowired
    private TrainerConnectRequestService trainerConnectRequestService;

    @Autowired
    private TrainerAvailabilityService trainerAvailabilityService;

    @Autowired
    private AppointmentBookingService appointmentBookingService;

    /**
     * Update the current trainer's profile using DTO.
     *
     * This method receives a TrainerProfileDTO object from the client,
     * retrieves the current trainer profile from the database using the user ID from JWT,
     * and updates the profile fields accordingly.
     *
     * @param trainerProfileDTO the profile data to update
     * @return a RestResult indicating success or failure
     */
    @PutMapping("/profile")
    public RestResult<?> updateTrainerProfile(@Valid @RequestBody TrainerProfileDTO trainerProfileDTO) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }

        // 修改 TrainerProfile 表中的记录，用DTO中的数据更新
        LambdaUpdateWrapper<TrainerProfile> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TrainerProfile::getUserId, currentUserId)
                .set(TrainerProfile::getCertifications, trainerProfileDTO.getCertifications())
                .set(TrainerProfile::getSpecializations, trainerProfileDTO.getSpecializations())
                .set(TrainerProfile::getYearsOfExperience, trainerProfileDTO.getYearsOfExperience())
                .set(TrainerProfile::getBiography, trainerProfileDTO.getBiography());

        boolean updateResult = trainerProfileService.update(updateWrapper);
        if (!updateResult) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Update failed: Trainer profile may not exist.");
        }
        log.info("Trainer [{}] profile updated successfully", currentUserId);
        return RestResult.success("Updated", "Trainer profile updated successfully.");
    }

    // 教练查看自己的详细信息表+教练user表中信息
    @GetMapping("/profile")
    public RestResult<?> getTrainerProfile() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        // 查两个表，一个是user表，一个是trainerProfile表
        TrainerAllProfile trainerAllProfile = trainerProfileService.getTrainerAllProfile(currentUserId);

        return RestResult.success(trainerAllProfile, "Trainer profile retrieved successfully.");
    }

    /**
     * 接受 member 的 connect 申请
     * 仅负责校验当前教练身份和调用业务层方法，具体逻辑在 Service 层处理
     *
     * @param decisionDTO 包含申请ID和可选反馈信息
     * @return 操作结果
     */
    @PutMapping("/connect-request/accept")
    public RestResult<?> acceptConnectRequest(@Valid @RequestBody TrainerConnectDecisionDTO decisionDTO) {
        Long currentTrainerId = SecurityUtils.getCurrentUserId();
        if (currentTrainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        trainerConnectRequestService.acceptConnectRequest(decisionDTO, currentTrainerId);
        log.info("Trainer [{}] accepted connect request [{}]", currentTrainerId, decisionDTO.getRequestId());
        return RestResult.success(null, "Connect request accepted successfully.");
    }

    /**
     * 拒绝 member 的 connect 申请
     * 仅负责校验当前教练身份和调用业务层方法，具体逻辑在 Service 层处理
     *
     * @param decisionDTO 包含申请ID和可选反馈信息
     * @return 操作结果
     */
    @PutMapping("/connect-request/reject")
    public RestResult<?> rejectConnectRequest(@Valid @RequestBody TrainerConnectDecisionDTO decisionDTO) {
        Long currentTrainerId = SecurityUtils.getCurrentUserId();
        if (currentTrainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        trainerConnectRequestService.rejectConnectRequest(decisionDTO, currentTrainerId);
        log.info("Trainer [{}] rejected connect request [{}]", currentTrainerId, decisionDTO.getRequestId());
        return RestResult.success(null, "Connect request rejected successfully.");
    }

    /**
     * 设置或更新教练的可用时间
     * 前端可以一次性传递多个可用时间段，例如在日历上勾选多个小时段后统一提交
     */
    @PostMapping("/availability")
    public RestResult<?> updateAvailability(@Valid @RequestBody TrainerAvailabilityDTO availabilityDTO) {
        Long currentTrainerId = SecurityUtils.getCurrentUserId();
        if (currentTrainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        // 这里假设用户ID转换为 Integer 类型（根据 TrainerAvailability 实体中的 trainerId 类型）
        trainerAvailabilityService.updateAvailability(currentTrainerId, availabilityDTO.getAvailabilitySlots());
        return RestResult.success(null, "Availability updated successfully.");
    }


    /**
     * 教练查询待审核预约请求接口（仅返回状态为 Pending 且未过期的预约）
     */
    @GetMapping("/appointments/pending")
    public RestResult<?> getPendingAppointments() {
        Long currentTrainerId = SecurityUtils.getCurrentUserId();
        if (currentTrainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        List<AppointmentBooking> pendingAppointments = appointmentBookingService.getPendingAppointmentsForTrainer(currentTrainerId);
        return RestResult.success(pendingAppointments, "Pending appointments retrieved successfully.");
    }


    /**
     * 这个是教练修改和初始化自己的可用时间接口
     * 查出教练的所有时间段，包括booked和unavailable（暂时没有unavailable）
     * 前端无需传递额外参数，直接通过 SecurityUtils 获取当前教练ID
     */
    @GetMapping("/availability")
    public RestResult<?> getAvailability() {
        Long currentTrainerId = SecurityUtils.getCurrentUserId();
        if (currentTrainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        List<TrainerAvailability> availabilities = trainerAvailabilityService.getFutureAvailability(currentTrainerId);
        return RestResult.success(availabilities, "Availability retrieved successfully.");
    }


    /**
     * 教练同意学员预约申请接口
     * 前端传入 AppointmentDecisionDTO，其中包含预约ID和可选反馈信息
     */
    @PutMapping("/appointment/accept")
    public RestResult<?> acceptAppointment(@Valid @RequestBody AppointmentDecisionDTO decisionDTO) {
        Long currentTrainerId = SecurityUtils.getCurrentUserId();
        if (currentTrainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        appointmentBookingService.acceptAppointment(decisionDTO, currentTrainerId);
        log.info("Trainer [{}] accepted appointment [{}]", currentTrainerId, decisionDTO.getAppointmentId());
        return RestResult.success(null, "Appointment accepted successfully.");
    }

    /**
     * 教练拒绝学员预约申请接口
     * 前端传入 AppointmentDecisionDTO，其中包含预约ID和可选反馈信息
     */
    @PutMapping("/appointment/reject")
    public RestResult<?> rejectAppointment(@Valid @RequestBody AppointmentDecisionDTO decisionDTO) {
        Long currentTrainerId = SecurityUtils.getCurrentUserId();
        if (currentTrainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        appointmentBookingService.rejectAppointment(decisionDTO, currentTrainerId);
        log.info("Trainer [{}] rejected appointment [{}]", currentTrainerId, decisionDTO.getAppointmentId());
        return RestResult.success(null, "Appointment rejected successfully.");
    }
}