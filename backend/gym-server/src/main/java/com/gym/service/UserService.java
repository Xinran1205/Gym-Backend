package com.gym.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.dto.SignupRequest;
import com.gym.dto.UserProfileDTO;
import com.gym.dto.VerifyCodeRequest;
import com.gym.entity.User;

import java.util.List;

public interface UserService extends IService<User> {
    // 注册相关
    void sendSignupVerification(SignupRequest signupRequest);
    void verifySignupCode(VerifyCodeRequest verifyReq);

    // 用户资料更新
    void updateUserProfile(Long userId, UserProfileDTO userProfileDTO);

    // 原有方法
    User createUser(User user);
    User getUserById(Long userID);
    User getByEmail(String email);
}

