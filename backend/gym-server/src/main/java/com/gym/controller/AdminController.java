package com.gym.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gym.dto.UserEmail;
import com.gym.entity.User;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.result.RestResult;
import com.gym.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/admin")
@Slf4j
// 这个地方就是利用了Spring Security的注解，只有拥有Admin角色的用户才能访问这个接口
// 也可以配置在securityConfig文件里面
@PreAuthorize("hasRole('admin')")
public class AdminController {

    @Autowired
    private UserService userService;

    /**
     * Approve user application
     */
    @PostMapping("/approve")
    public RestResult<?> approveApplication(@Valid @RequestBody UserEmail userEmail) {
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getEmail, userEmail.getEmail())
                .set(User::getAccountStatus, User.AccountStatus.Approved);

        boolean updateResult = userService.update(updateWrapper);
        if (!updateResult) {
            // If user not found or DB error
            throw new CustomException(ErrorCode.BAD_REQUEST, "Failed to approve. Possibly user not found.");
        }

        return RestResult.success("Approved", "User application approved successfully.");
    }

    /**
     * Get the number of pending users
     */
    @GetMapping("/pendingNum")
    public RestResult<?> pendingNotification() {
        // Optionally, you can define getPendingUserCount() in service
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getAccountStatus, User.AccountStatus.Pending);
        int pendingNum = userService.count(queryWrapper);

        return RestResult.success(pendingNum, "Number of pending users retrieved successfully.");
    }

    /**
     * Reject user application
     */
    @PostMapping("/reject")
    public RestResult<?> rejectApplication(@Valid @RequestBody UserEmail userEmail) {
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getEmail, userEmail.getEmail())
                .set(User::getAccountStatus, User.AccountStatus.Suspended);

        boolean updateResult = userService.update(updateWrapper);
        if (!updateResult) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Failed to reject. Possibly user not found.");
        }

        return RestResult.success("Rejected", "User application rejected (suspended) successfully.");
    }

}
