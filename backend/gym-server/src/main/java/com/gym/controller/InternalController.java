package com.gym.controller;

import com.gym.entity.User;
import com.gym.result.RestResult;
import com.gym.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 内部服务调用控制器
 * 
 * 功能说明：
 * 1. 提供内部微服务间调用的API接口
 * 2. 主要供认证服务等其他服务调用
 * 3. 不对外暴露，仅用于服务间通信
 * 
 * @author gym-system
 * @version 1.0
 */
@Api(tags = "内部服务调用接口")
@RestController
@RequestMapping("/api/internal")
@Slf4j
public class InternalController {

    @Autowired
    private UserService userService;

    /**
     * 根据用户ID获取用户信息（内部调用）
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    @ApiOperation("根据用户ID获取用户信息")
    @GetMapping("/users/{userId}")
    public RestResult<User> getUserById(@PathVariable Long userId) {
        try {
            log.debug("内部调用：根据用户ID获取用户信息 - userId: {}", userId);
            User user = userService.getUserById(userId);
            
            if (user != null) {
                log.debug("内部调用成功：找到用户信息 - userId: {}, email: {}", userId, user.getEmail());
                return RestResult.success(user);
            } else {
                log.debug("内部调用：用户不存在 - userId: {}", userId);
                return RestResult.error("用户不存在", 404);
            }
        } catch (Exception e) {
            log.error("内部调用异常：获取用户信息失败 - userId: {}, error: {}", userId, e.getMessage());
            return RestResult.error("获取用户信息失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 根据邮箱获取用户信息（内部调用）
     * 
     * @param email 用户邮箱
     * @return 用户信息
     */
    @ApiOperation("根据邮箱获取用户信息")
    @GetMapping("/users/email/{email}")
    public RestResult<User> getUserByEmail(@PathVariable String email) {
        try {
            log.debug("内部调用：根据邮箱获取用户信息 - email: {}", email);
            User user = userService.getByEmail(email);
            
            if (user != null) {
                log.debug("内部调用成功：找到用户信息 - email: {}, userId: {}", email, user.getUserID());
                return RestResult.success(user);
            } else {
                log.debug("内部调用：用户不存在 - email: {}", email);
                return RestResult.error("用户不存在", 404);
            }
        } catch (Exception e) {
            log.error("内部调用异常：获取用户信息失败 - email: {}, error: {}", email, e.getMessage());
            return RestResult.error("获取用户信息失败: " + e.getMessage(), 500);
        }
    }
}
