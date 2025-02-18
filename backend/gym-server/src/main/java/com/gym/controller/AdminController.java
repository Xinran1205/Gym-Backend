package com.gym.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gym.dto.UserEmail;
import com.gym.entity.User;
import com.gym.result.RestResult;
import com.gym.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@Slf4j
// 这个地方就是利用了Spring Security的注解，只有拥有Admin角色的用户才能访问这个接口
// 也可以配置在securityConfig文件里面
@PreAuthorize("hasRole('Admin')")
public class AdminController {

    @Autowired
    private UserService userService;
    // 管理员审批申请
    // 返回值：成功或失败
    // 接收值：申请的用户邮箱

    @PostMapping("/approve")
    public RestResult<?> approveApplication(@RequestBody UserEmail email){
        // 1. 已经校验了只有管理员才能访问这个接口

        // 2. 根据邮箱找到用户？
        // 根据id找到用户，修改用户状态，把accountStatus改为AccountStatus.Approved
        User curUser = userService.getByEmail(email.getEmail());
        if(curUser == null){
            return RestResult.error("User not found", null);
        }
        curUser.setAccountStatus(User.AccountStatus.Approved);
        userService.updateById(curUser);

        // 3. 返回成功或失败
        return RestResult.success("Approved", null);
    }

    // 查出待审核的用户有多少条，数字返回给前端作为notification
    @GetMapping("/pendingNum")
    public RestResult<?> pendingNotification(){
        // 使用 MyBatis-Plus 的 LambdaQueryWrapper 构造条件查询待审核用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getAccountStatus, User.AccountStatus.Pending);

        // 直接通过 count 方法查询待审核用户的数量
        int pendingNum = userService.count(queryWrapper);

        // 将查询结果返回给前端
        return RestResult.success(pendingNum, "Pending users number");
    }

    // 拒绝用户申请
    // 设置成Suspended
    @PostMapping("/reject")
    public RestResult<?> rejectApplication(@RequestBody Long userID){
        // 构造更新条件：根据用户ID找到记录，并将accountStatus设置为Rejected
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUserID, userID)
                .set(User::getAccountStatus, User.AccountStatus.Suspended);

        // 执行更新操作，不需要先查询
        boolean updateResult = userService.update(updateWrapper);
        if (!updateResult) {
            return RestResult.error("Update failed", null);
        }

        // 返回操作结果
        return RestResult.success("Rejected", null);
    }
    
}
