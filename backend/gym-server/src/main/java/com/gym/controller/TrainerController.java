package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gym.dto.TrainerProfileDTO;
import com.gym.dto.UserEmail;
import com.gym.entity.TrainerProfile;
import com.gym.entity.User;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.result.RestResult;
import com.gym.service.TrainerProfileService;
import com.gym.service.UserService;
import com.gym.util.SecurityUtils;
import com.gym.vo.TrainerAllProfile;
import com.gym.vo.UserProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;

@RestController
@RequestMapping("/trainer")
@Slf4j
@PreAuthorize("hasRole('Trainer')")
public class TrainerController {

    @Autowired
    private TrainerProfileService trainerProfileService;

    @Autowired
    private UserService userService;

    /**
     * Update the current trainer's profile using DTO.
     *
     * This method receives a TrainerProfileDTO object from the client,
     * retrieves the current trainer profile from the database using the user ID from JWT,
     * and updates the profile fields accordingly.
     *
     * @param trainerProfileDTO the profile data to update
     * @return a RestResult indicating success or failure
     */
    @PutMapping("/profile")
    public RestResult<?> updateTrainerProfile(@Valid @RequestBody TrainerProfileDTO trainerProfileDTO) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }

        // 修改 TrainerProfile 表中的记录，用DTO中的数据更新
        LambdaUpdateWrapper<TrainerProfile> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TrainerProfile::getUserId, currentUserId)
                .set(TrainerProfile::getCertifications, trainerProfileDTO.getCertifications())
                .set(TrainerProfile::getSpecializations, trainerProfileDTO.getSpecializations())
                .set(TrainerProfile::getYearsOfExperience, trainerProfileDTO.getYearsOfExperience())
                .set(TrainerProfile::getBiography, trainerProfileDTO.getBiography());

        boolean updateResult = trainerProfileService.update(updateWrapper);
        if (!updateResult) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Update failed: Trainer profile may not exist.");
        }
        log.info("Trainer [{}] profile updated successfully", currentUserId);
        return RestResult.success("Updated", "Trainer profile updated successfully.");
    }

    // 教练查看自己的详细信息表+教练user表中信息
    @GetMapping("/profile")
    public RestResult<?> getTrainerProfile() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        // 查两个表，一个是user表，一个是trainerProfile表
        TrainerAllProfile trainerAllProfile = trainerProfileService.getTrainerAllProfile(currentUserId);

        return RestResult.success(trainerAllProfile, "Trainer profile retrieved successfully.");
    }
}