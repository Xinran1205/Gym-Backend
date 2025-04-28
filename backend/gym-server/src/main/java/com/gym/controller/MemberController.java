package com.gym.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.dto.*;
import com.gym.entity.User;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.result.RestResult;
import com.gym.service.*;
import com.gym.util.SecurityUtils;
import com.gym.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/member")
@Slf4j
@PreAuthorize("hasRole('member')")
public class MemberController {

    @Autowired
    private UserService userService;

    @Autowired
    private TrainerProfileService trainerProfileService;

    @Autowired
    private TrainerConnectRequestService trainerConnectRequestService;

    @Autowired
    private TrainerAvailabilityService trainerAvailabilityService;

    @Autowired
    private AppointmentBookingService appointmentBookingService;

    // 分页查询教练列表
    // 这个应该是在membercontroller，得是member权限才能看到
    @GetMapping("/listTrainers")
    public RestResult<?> listTrainers(TrainerProfileQuery query) {
        Page<TrainerProfileVO> resultPage = trainerProfileService.listTrainers(query);
        return RestResult.success(resultPage, "Trainer list retrieved successfully.");
    }

    // member 查看自己的简单信息
    @GetMapping("/user-profile")
    public RestResult<?> getUserProfile() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        // 根据当前用户ID查询 User 表中的记录
        User user = userService.getById(currentUserId);
        if (user == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "User not found.");
        }

        // vo 类，将 User 对象转换为 UserProfileResponse 对象
        UserProfileResponse response = UserProfileResponse.builder()
                .name(user.getName())
                .dateOfBirth(user.getDateOfBirth())
                .address(user.getAddress())
                .email(user.getEmail())
                .address(user.getAddress())
                .role(user.getRole())
                .isGoogle(user.getPasswordHash() == null)
                .build();
        return RestResult.success(response, "User profile retrieved successfully.");
    }

    // 新增提交 connect 申请接口
    @PostMapping("/connect-trainer")
    public RestResult<?> connectTrainer(@RequestBody @Valid TrainerConnectRequestDTO requestDTO) {
        // 获取当前用户 ID
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        // 判断当前 member 待审核申请是否超过 5 个
        int pendingCount = trainerConnectRequestService.countPendingRequests(currentUserId);
        if (pendingCount >= 5) {
            throw new CustomException(ErrorCode.TRAINER_REQUEST_LIMIT, "You have reached the maximum number of pending requests.");
        }
        // 提交申请，状态为 Pending
        trainerConnectRequestService.submitConnectRequest(requestDTO, currentUserId);
        return RestResult.success(null, "Connect request submitted successfully.");
    }

    /**
     * 用户查询指定教练的未来可用时间段接口
     * 前端需要传入教练的ID，该接口仅返回状态为 Available 且开始时间在当前时间之后的时间段
     */

    @GetMapping("/trainer/{trainerId}/availability")
    public RestResult<?> getTrainerAvailability(@PathVariable("trainerId") Long trainerId) {
        // 校验当前用户（学员）是否登录
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }

        // 调用 service 层查询指定教练从当前时间开始、状态为 Available 的可用时间段
        List<AvailabilitySlotDTO> slotList = trainerAvailabilityService.getAvailableSlots(trainerId);

        // 封装为 TrainerAvailabilityDTO
        TrainerAvailabilityDTO responseDTO = TrainerAvailabilityDTO.builder()
                .availabilitySlots(slotList)
                .build();

        return RestResult.success(responseDTO, "Trainer availability retrieved successfully.");
    }

    // 会员选择教练的可用时间段并提交预约请求
    @PostMapping("/appointment")
    public RestResult<?> bookAppointment(@RequestBody @Valid AppointmentBookingDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        appointmentBookingService.bookSession(dto, currentUserId);
        return RestResult.success(null, "Appointment booking submitted successfully.");
    }


    /**
     * 分页查询当前会员所有未来预约的详细信息：
     * 先自动更新过期（或完成）的记录，然后只返回状态为 Pending 和 Approved 的记录。
     */

//    这个设计的核心思路是：在会员查询预约记录之前，先统一批量更新所有待处理记录的状态
//    （例如将已到时间但状态仍为 Pending 的记录更新为 Expired，或将 Approved 但课程结束的更新为 Completed），然后再进行分页查询。
    @GetMapping("/appointments/upcoming")
    public RestResult<?> getUpcomingAppointments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize, String status) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        Page<AppointmentBookingDetailVO> appointmentPage = new Page<>(page, pageSize);
        appointmentPage = appointmentBookingService.getUpcomingAppointmentsForMember(currentUserId, appointmentPage, status);
        return RestResult.success(appointmentPage, "Upcoming appointments retrieved successfully.");
    }

    /**
     * 分页查询当前会员的历史预约记录：
     * 返回状态不为 Pending 和 Approved 的记录（例如 Expired, Rejected, Cancelled, Completed）。
     */
    @GetMapping("/appointments/history")
    public RestResult<?> getHistoricalAppointments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize, String status) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        Page<AppointmentBookingHistoryDetailVO> appointmentPage = new Page<>(page, pageSize);
        appointmentPage = appointmentBookingService.getHistoricalAppointmentsForMember(currentUserId, appointmentPage, status);
        return RestResult.success(appointmentPage, "Historical appointments retrieved successfully.");
    }

    /**
     * 会员取消预约接口
     * 会员可以取消自己状态为 Pending 的预约；如果预约已被批准（Approved），则提示用户不能直接取消
     * 预约ID 通过 URL 路径传入
     */
    @PutMapping("/appointment/cancel/{appointmentId}")
    public RestResult<?> cancelAppointment(@PathVariable("appointmentId") Long appointmentId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        appointmentBookingService.cancelAppointment(appointmentId, currentUserId);
        return RestResult.success(null, "Appointment cancelled successfully.");
    }

    /**
     * 动态统计接口：查询当前会员在指定日期范围内的预约统计数据，
     * 返回每天完成的课程小时数（单位：小时）。
     * 前端可用于展示图表，日期范围最大不超过30天。
     * 每条数据是30min
     *
     * @param startDate 统计开始日期（格式 yyyy-MM-dd）
     * @param endDate   统计结束日期（格式 yyyy-MM-dd）
     */
    @GetMapping("/appointments/statistics/dynamic")
    public RestResult<?> getDynamicAppointmentStatistics(
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        DynamicAppointmentStatisticsVO statistics = appointmentBookingService.getDynamicAppointmentStatisticsForMember(currentUserId, startDate, endDate);
        return RestResult.success(statistics, "Appointment statistics retrieved successfully.");
    }

    /**
     * 判断当前会员是否已与指定教练建立连接（即连接申请被接受）
     *
     * @param trainerId 教练的ID，由前端传入
     * @return 连接状态，true 表示已连接，false 表示未连接
     */
    @GetMapping("/is-connected/{trainerId}")
    public RestResult<?> isConnectedWithTrainer(@PathVariable("trainerId") Long trainerId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        boolean connected = trainerConnectRequestService.isConnected(currentUserId, trainerId);
        return RestResult.success(connected, "Connection status retrieved successfully.");
    }

}
