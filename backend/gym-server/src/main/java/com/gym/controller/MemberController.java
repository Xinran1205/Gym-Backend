package com.gym.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.dto.TrainerProfileQuery;
import com.gym.entity.User;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.result.RestResult;
import com.gym.service.TrainerProfileService;
import com.gym.service.UserService;
import com.gym.util.SecurityUtils;
import com.gym.vo.TrainerProfileVO;
import com.gym.vo.UserProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/member")
@Slf4j
@PreAuthorize("hasRole('Member')")
public class MemberController {

    @Autowired
    private UserService userService;

    @Autowired
    private TrainerProfileService trainerProfileService;

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
                .build();
        return RestResult.success(response, "User profile retrieved successfully.");
    }
}
