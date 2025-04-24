package com.gym.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.dto.SignupRequest;
import com.gym.dto.UserProfileDTO;
import com.gym.dto.VerifyCodeRequest;
import com.gym.entity.Specializations;
import com.gym.entity.User;
import com.gym.vo.TrainerBasicInfoVO;

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

    /**
     * 查询所有专长常量
     * @return List of Specializations
     */
    List<Specializations> listSpecializations();

    /**
     * 查询所有教练（排除自己），只返回 trainerId 和 name
     */
    List<TrainerBasicInfoVO> listOtherTrainersBasicInfo(Long excludeTrainerId);

    /**
     * Paginated query for all users whose accountStatus is Pending.
     *
     * @param page Page object (current, size)
     * @return paged result with pending users
     */
    Page<User> getPendingUsers(Page<User> page);

    /**
     * 条件分页查询用户（仅 member 和 trainer），role 可选：member/trainer，不传则查询两者
     *
     * @param page     页码（从 1 开始）
     * @param pageSize 每页记录数
     * @param role     可选，"member" 或 "trainer"
     * @return 分页结果
     */
    Page<User> listUsers(int page, int pageSize, String role);
}

