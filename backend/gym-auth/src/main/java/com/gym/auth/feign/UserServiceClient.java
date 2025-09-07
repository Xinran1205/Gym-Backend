package com.gym.auth.feign;

import com.gym.entity.User;
import com.gym.result.RestResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务Feign客户端
 * 
 * 功能说明：
 * 1. 通过Feign调用gym-server服务获取用户信息
 * 2. 支持负载均衡和服务发现
 * 3. 提供用户认证所需的用户数据
 * 
 * @author gym-system
 * @version 1.0
 */
@FeignClient(name = "gym-server", path = "/api")
public interface UserServiceClient {

    /**
     * 根据用户ID获取用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/internal/users/{userId}")
    RestResult<User> getUserById(@PathVariable("userId") Long userId);

    /**
     * 根据邮箱获取用户信息
     * 
     * @param email 用户邮箱
     * @return 用户信息
     */
    @GetMapping("/internal/users/email/{email}")
    RestResult<User> getUserByEmail(@PathVariable("email") String email);
}
