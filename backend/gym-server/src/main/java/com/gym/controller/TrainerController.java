package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.dto.*;
import com.gym.entity.*;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.result.RestResult;
import com.gym.service.*;
import com.gym.util.SecurityUtils;
import com.gym.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
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

    @Autowired
    private UserService userService;

    @Autowired
    private WorkoutPlanService workoutPlanService;

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
     * 教练查询待审核预约请求接口（Pending 且未过期），返回课程开始/结束时间
     */
    @GetMapping("/appointments/pending")
    public RestResult<?> getPendingAppointments() {
        Long trainerId = SecurityUtils.getCurrentUserId();
        if (trainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED,
                    "User is not authenticated or session is invalid.");
        }
        List<PendingAppointmentVO> pending =
                appointmentBookingService.getPendingAppointmentsForTrainerWithTimes(trainerId);
        return RestResult.success(pending, "Pending appointments retrieved successfully.");
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
    public RestResult<?> rejectAppointment(@Valid @RequestBody AppointmentDecisionRejectDTO decisionDTO) {
        Long currentTrainerId = SecurityUtils.getCurrentUserId();
        if (currentTrainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        appointmentBookingService.rejectAppointment(decisionDTO, currentTrainerId);
        log.info("Trainer [{}] rejected appointment [{}]", currentTrainerId, decisionDTO.getAppointmentId());
        return RestResult.success(null, "Appointment rejected successfully.");
    }


    /**
     * 教练查询所有已批准（Approved）的预约，包含学员姓名与课程时段
     */
    @GetMapping("/appointments/approved")
    public RestResult<?> getApprovedAppointments() {
        Long trainerId = SecurityUtils.getCurrentUserId();
        if (trainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED,
                    "User is not authenticated or session is invalid.");
        }
        List<PendingAppointmentVO> approved =
                appointmentBookingService.getApprovedAppointmentsForTrainerWithTimes(trainerId);
        return RestResult.success(approved, "Approved appointments retrieved successfully.");
    }

    /**
     * 教练将指定预约标记为 Completed
     */
    @PutMapping("/appointment/complete")
    public RestResult<?> completeAppointment(@Valid @RequestBody AppointmentCompleteDTO completeDTO) {
        Long currentTrainerId = SecurityUtils.getCurrentUserId();
        if (currentTrainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        appointmentBookingService.completeAppointment(completeDTO.getAppointmentId(), currentTrainerId);
        log.info("Trainer [{}] completed appointment [{}]", currentTrainerId, completeDTO.getAppointmentId());
        return RestResult.success(null, "Appointment marked as completed successfully.");
    }

    /**
     * 获取可用于推荐的其他教练（基础信息）
     * GET /trainer/alternative-trainers
     */
    @GetMapping("/alternative-trainers")
    public RestResult<?> getAlternativeTrainers() {
        Long currentTrainerId = SecurityUtils.getCurrentUserId();
        if (currentTrainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED,
                    "User is not authenticated or session is invalid.");
        }
        List<TrainerBasicInfoVO> trainers =
                userService.listOtherTrainersBasicInfo(currentTrainerId);

        return RestResult.success(trainers,
                "Alternative trainers retrieved successfully.");
    }

    /**
     * 教练查询所有待审核的连接申请
     * GET /trainer/connect-requests/pending
     */
    @GetMapping("/connect-requests/pending")
    public RestResult<?> getPendingConnectRequests() {
        Long trainerId = SecurityUtils.getCurrentUserId();
        if (trainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED,
                    "User is not authenticated or session is invalid.");
        }

        List<PendingConnectRequestVO> pending =
                trainerConnectRequestService.getPendingConnectRequestsForTrainer(trainerId);

        return RestResult.success(pending,
                "Pending connect requests retrieved successfully.");
    }

    /**
     * 获取教练全部预约，并按学员分组
     * GET /trainer/appointments/by-member
     */
    @GetMapping("/appointments/by-member")
    public RestResult<?> getAllAppointmentsGroupedByMember() {
        Long currentTrainerId = SecurityUtils.getCurrentUserId();
        if (currentTrainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED,
                    "User is not authenticated or session is invalid.");
        }
        List<MemberAppointmentsVO> grouped =
                appointmentBookingService.getAllAppointmentsGroupedByMember(currentTrainerId);

        return RestResult.success(grouped,
                "All appointments grouped by member retrieved successfully.");
    }

    /**
     * 教练查询已连接 (Accepted) 的全部会员
     * GET /trainer/connected-members
     */
    @GetMapping("/connected-members")
    public RestResult<?> getConnectedMembers() {
        Long trainerId = SecurityUtils.getCurrentUserId();
        if (trainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED,
                    "User is not authenticated or session is invalid.");
        }
        List<ConnectedMemberVO> members =
                trainerConnectRequestService.listConnectedMembers(trainerId);

        return RestResult.success(members,
                "Connected members retrieved successfully.");
    }

    /**
     * 教练查询所有已完成的预约（历史查询），包含学员姓名
     * GET /trainer/appointments/completed
     */
    @GetMapping("/appointments/completed")
    public RestResult<?> getCompletedAppointments() {
        Long trainerId = SecurityUtils.getCurrentUserId();
        if (trainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED,
                    "User is not authenticated or session is invalid.");
        }
        List<CompletedAppointmentVO> completed =
                appointmentBookingService.getCompletedAppointmentsForTrainer(trainerId);
        return RestResult.success(completed,
                "Completed appointments retrieved successfully.");
    }

    /**
     * 教练端：指定日期区间内每天完成的课时（≤30天）
     * GET /trainer/appointments/statistics/dynamic
     * ?startDate=2025-04-01&endDate=2025-04-07
     */
    @GetMapping("/appointments/statistics/dynamic")
    public RestResult<?> getDynamicTrainerStatistics(
            @RequestParam("startDate")
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate")
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        Long trainerId = SecurityUtils.getCurrentUserId();
        if (trainerId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED,
                    "User is not authenticated or session is invalid.");
        }

        DynamicAppointmentStatisticsVO vo =
                appointmentBookingService.getDynamicAppointmentStatisticsForTrainer(
                        trainerId, startDate, endDate);

        return RestResult.success(vo,
                "Trainer appointment statistics retrieved successfully.");
    }


    // 7‑1  CRUD workout plans
    @PostMapping("/workout-plans")
    public RestResult<?> createPlan(@Valid @RequestBody WorkoutPlanDTO dto) {
        Long trainerId = SecurityUtils.getCurrentUserId();
        return RestResult.success(
                workoutPlanService.createPlan(trainerId, dto),
                "Plan created");
    }

    @PutMapping("/workout-plans/{planId}")
    public RestResult<?> updatePlan(@PathVariable Long planId,
                                    @Valid @RequestBody WorkoutPlanDTO dto) {
        Long trainerId = SecurityUtils.getCurrentUserId();
        return RestResult.success(
                workoutPlanService.updatePlan(trainerId, planId, dto),
                "Plan updated");
    }

    @DeleteMapping("/workout-plans/{planId}")
    public RestResult<?> deletePlan(@PathVariable Long planId) {
        Long trainerId = SecurityUtils.getCurrentUserId();
        workoutPlanService.deletePlan(trainerId, planId);
        return RestResult.success(null, "Plan deleted");
    }

    @GetMapping("/workout-plans")
    public RestResult<?> listPlans() {
        Long trainerId = SecurityUtils.getCurrentUserId();
        return RestResult.success(
                workoutPlanService.listPlans(trainerId),
                "Plans retrieved");
    }

    // 7‑2  Bind a plan to an appointment
    @PutMapping("/appointments/{appointmentId}/workout-plan/{planId}")
    public RestResult<?> bindPlan(@PathVariable Long appointmentId,
                                  @PathVariable Long planId) {
        Long trainerId = SecurityUtils.getCurrentUserId();
        appointmentBookingService.bindWorkoutPlan(trainerId, appointmentId, planId);
        return RestResult.success(null, "Workout plan bound to appointment");
    }

}