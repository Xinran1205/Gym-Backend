package com.gym.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.dto.*;
import com.gym.entity.Notification;
import com.gym.entity.TrainerAvailability;
import com.gym.entity.User;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.result.RestResult;
import com.gym.service.*;
import com.gym.util.SecurityUtils;
import com.gym.vo.TrainerProfileVO;
import com.gym.vo.UserProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
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

    // 用户选择教练的可用时间段并提交预约请求
    @PostMapping("/appointment")
    public RestResult<?> bookAppointment(@RequestBody @Valid AppointmentBookingDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        appointmentBookingService.bookSession(dto, currentUserId);
        return RestResult.success(null, "Appointment booking submitted successfully.");
    }

}
