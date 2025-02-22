package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.dao.TrainerProfileDao;
import com.gym.dao.UserDao;
import com.gym.entity.TrainerProfile;
import com.gym.entity.User;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.service.TrainerProfileService;
import com.gym.service.UserService;
import com.gym.vo.TrainerAllProfile;
import com.gym.vo.UserProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TrainerProfileServiceImpl extends ServiceImpl<TrainerProfileDao, TrainerProfile>
        implements TrainerProfileService {

    @Autowired
    UserService userService;

    /**
     * 为指定的教练用户创建一条默认的 TrainerProfile 记录。
     * 默认值均为空（或 0），后续教练可以通过完善页面进行修改。
     *
     * @param userId 教练用户的ID
     */
    @Override
    public void createDefaultTrainerProfile(Long userId) {
        // 使用builder
        TrainerProfile trainerProfile = TrainerProfile.builder()
                .userId(userId)
                .certifications("")
                .specializations("")
                .yearsOfExperience(0)
                .biography("")
                .build();
        // 保存记录到数据库
        this.save(trainerProfile);
    }

    // 查询教练的profile和自己的简单信息
    @Override
    public TrainerAllProfile getTrainerAllProfile(Long currentUserId){
        // 根据当前用户ID查询 User 表中的记录
        User user = userService.getById(currentUserId);
        if (user == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "User not found.");
        }
        UserProfileResponse userProfileResponse = UserProfileResponse.builder()
                .name(user.getName())
                .dateOfBirth(user.getDateOfBirth())
                .address(user.getAddress())
                .build();
        // 根据当前用户ID查询 TrainerProfile 表中的记录,用户id是profile表的外键
        LambdaQueryWrapper<TrainerProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrainerProfile::getUserId, currentUserId);
        TrainerProfile trainerProfile = this.getOne(queryWrapper);
        if (trainerProfile == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Trainer profile not found.");
        }

        // 使用build
        return TrainerAllProfile.builder()
                .trainerProfile(trainerProfile)          // 设置 TrainerProfile 对象
                .userProfileResponse(userProfileResponse) // 设置 UserProfileResponse 对象
                .build();                               // 构建最终对象
    }
}

