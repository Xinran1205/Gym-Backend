package com.gym.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.dto.TrainerConnectRequestDTO;
import com.gym.dto.TrainerProfileQuery;
import com.gym.entity.Notification;
import com.gym.entity.User;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.result.RestResult;
import com.gym.service.NotificationService;
import com.gym.service.TrainerConnectRequestService;
import com.gym.service.TrainerProfileService;
import com.gym.service.UserService;
import com.gym.util.SecurityUtils;
import com.gym.vo.TrainerProfileVO;
import com.gym.vo.UserProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;


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
    private NotificationService notificationService;

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

    // 新增接口：分页查询当前 member 的通知列表（业务逻辑在 service 层）
    @GetMapping("/notifications")
    public RestResult<?> getNotifications(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        // 这里我就全部返回了！
        Page<Notification> notificationsPage = notificationService.getNotificationsByUser(currentUserId, page, pageSize);
        return RestResult.success(notificationsPage, "Notifications retrieved successfully.");
    }

    // 新增接口：标记指定通知为已读（业务逻辑在 service 层）
    @PutMapping("/notifications/{notificationId}/read")
    public RestResult<?> markNotificationAsRead(@PathVariable("notificationId") Long notificationId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        notificationService.markAsRead(notificationId, currentUserId);
        return RestResult.success(null, "Notification marked as read successfully.");
    }

    // 新增删除通知接口：仅允许删除已读通知
    @DeleteMapping("/notifications/{notificationId}")
    public RestResult<?> deleteNotification(@PathVariable("notificationId") Long notificationId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        notificationService.deleteNotification(notificationId, currentUserId);
        return RestResult.success(null, "Notification deleted successfully.");
    }
}
