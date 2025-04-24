package com.gym.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.dto.FitnessCentreDTO;
import com.gym.dto.SpecializationDTO;
import com.gym.dto.UserEmail;
import com.gym.entity.FitnessCentre;
import com.gym.entity.Specializations;
import com.gym.entity.User;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.result.RestResult;
import com.gym.service.FitnessCentreService;
import com.gym.service.SpecializationsService;
import com.gym.service.UserService;
import com.gym.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/admin")
@Slf4j
// 这个地方就是利用了Spring Security的注解，只有拥有Admin角色的用户才能访问这个接口
// 也可以配置在securityConfig文件里面
@PreAuthorize("hasRole('admin')")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private SpecializationsService specializationsService;  // 新增

    @Autowired
    private FitnessCentreService fitnessCentreService;  // 新增

    /**
     * 获取当前登录管理员的个人信息
     * GET /admin/profile
     */
    @GetMapping("/profile")
    public RestResult<User> getAdminProfile() {
        Long adminId = SecurityUtils.getCurrentUserId();
        if (adminId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED,
                    "User is not authenticated or session is invalid.");
        }
        User admin = userService.getUserById(adminId);
        if (admin == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Admin user not found.");
        }
        return RestResult.success(admin, "Admin profile retrieved successfully.");
    }

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

    /**
     * List all users whose account is pending approval
     *
     * Example: GET /admin/pending-users?page=1&pageSize=20
     */
    @GetMapping("/pending-users")
    public RestResult<?> listPendingUsers(
            @RequestParam(defaultValue = "1")      int page,
            @RequestParam(defaultValue = "10")     int pageSize) {

        Page<User> result = userService.getPendingUsers(new Page<>(page, pageSize));
        return RestResult.success(result,
                "Pending users retrieved successfully.");
    }

    /**
     * 列出所有 Specializations
     * GET /admin/specializations
     */
    @GetMapping("/specializations")
    public RestResult<?> listSpecializations() {
        List<Specializations> list = specializationsService.listAllSpecializations();
        return RestResult.success(list, "Specializations retrieved successfully.");
    }

    /**
     * 管理员新增一个 Specialization
     * POST /admin/specializations
     */
    @PostMapping("/specializations")
    public RestResult<?> addSpecialization(
            @Valid @RequestBody SpecializationDTO dto) {
        Specializations created = specializationsService.addSpecialization(dto);
        return RestResult.success(created, "Specialization added successfully.");
    }

    /**
     * 管理员删除一个 Specialization
     * DELETE /admin/specializations/{id}
     */
    @DeleteMapping("/specializations/{id}")
    public RestResult<?> deleteSpecialization(@PathVariable("id") Long id) {
        specializationsService.deleteSpecialization(id);
        return RestResult.success(null, "Specialization deleted successfully.");
    }



    /**
     * List all fitness centres
     * GET /admin/fitness-centres
     */
    @GetMapping("/fitness-centres")
    public RestResult<?> listFitnessCentres() {
        List<FitnessCentre> centres = fitnessCentreService.listAllCentres();
        return RestResult.success(centres, "Fitness centres retrieved successfully.");
    }

    /**
     * 这个可能不需要，废弃！
     * Get a fitness centre by ID
     * GET /admin/fitness-centres/{id}
     */
    @GetMapping("/fitness-centres/{id}")
    public RestResult<?> getFitnessCentre(@PathVariable Long id) {
        FitnessCentre centre = fitnessCentreService.getCentreById(id);
        return RestResult.success(centre, "Fitness centre retrieved successfully.");
    }

    /**
     * Create a new fitness centre
     * POST /admin/fitness-centres
     */
    @PostMapping("/fitness-centres")
    public RestResult<?> addFitnessCentre(@Valid @RequestBody FitnessCentreDTO dto) {
        FitnessCentre created = fitnessCentreService.addCentre(dto);
        return RestResult.success(created, "Fitness centre created successfully.");
    }

    /**
     * Update an existing fitness centre
     * PUT /admin/fitness-centres/{id}
     */
    @PutMapping("/fitness-centres/{id}")
    public RestResult<?> updateFitnessCentre(
            @PathVariable Long id,
            @Valid @RequestBody FitnessCentreDTO dto) {
        FitnessCentre updated = fitnessCentreService.updateCentre(id, dto);
        return RestResult.success(updated, "Fitness centre updated successfully.");
    }

    /**
     * Delete a fitness centre
     * DELETE /admin/fitness-centres/{id}
     */
    @DeleteMapping("/fitness-centres/{id}")
    public RestResult<?> deleteFitnessCentre(@PathVariable Long id) {
        fitnessCentreService.deleteCentre(id);
        return RestResult.success(null, "Fitness centre deleted successfully.");
    }

    /**
     * 条件分页查询所有 member 或 trainer 用户
     *
     * GET /admin/users?role={member|trainer}&page=1&pageSize=10
     */
    @GetMapping("/users")
    public RestResult<Page<User>> listUsers(
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        Page<User> result = userService.listUsers(page, pageSize, role);
        return RestResult.success(result, "Users retrieved successfully.");
    }
}
