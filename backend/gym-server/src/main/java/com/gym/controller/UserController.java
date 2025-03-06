package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.AOP.RateLimit;
import com.gym.bloomFilter.BloomFilterUtil;
import com.gym.dto.*;
import com.gym.dto.redis.PendingPasswordReset;
import com.gym.entity.*;
import com.gym.service.*;
import com.gym.service.impl.RedisCacheServiceImpl;
import com.gym.util.IpUtil;
import com.gym.util.SecurityUtils;
import com.gym.util.TencentCaptchaUtil;
import com.gym.vo.FitnessCentreVO;
import com.gym.vo.LoginResponse;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.result.RestResult;
import com.gym.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private TencentCaptchaUtil tencentCaptchaUtil;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FitnessCentreService fitnessCentreService;

    // 用于生成腾讯机器人验证码
    private void validateCaptcha(String captchaTicket, String captchaRandstr, HttpServletRequest request) {
        String clientIp = IpUtil.getClientIp(request);
        boolean captchaValid = tencentCaptchaUtil.verifyCaptcha(captchaTicket, captchaRandstr, clientIp);
        if (!captchaValid) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Captcha verification failed.");
        }
    }

    /**
     * 注册第一步：发送验证码邮件
     */
    @PostMapping("/signup")
    // 限流注解，60 秒内最多 5 次请求
    @RateLimit(timeWindowSeconds = 60, maxRequests = 5,
            message = "Too many signup requests. Please try again later.")
    public RestResult<?> signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        // 先进行验证码校验
        // validateCaptcha(request.getCaptchaTicket(), request.getCaptchaRandstr(), httpRequest);

        log.info("signup request: {}", request);
        userService.sendSignupVerification(request);
        return RestResult.success(null, "Verification code has been sent to your email. Please enter it to complete registration.");
    }

    /**
     * 注册第二步：验证验证码并完成注册
     */
    @PostMapping("/verify-code")
    public RestResult<?> verifyCode(@Valid @RequestBody VerifyCodeRequest verifyReq) {
        userService.verifySignupCode(verifyReq);
        return RestResult.success(null, "Registration successful. Awaiting admin approval.");
    }

    /**
     * 登录接口（包含JWT认证）
     */
    @PostMapping("/login")
    public RestResult<LoginResponse> login(@Valid @RequestBody LoginRequest loginReq,
                                           HttpServletRequest httpRequest) {
        // 先进行验证码校验
        // validateCaptcha(loginReq.getCaptchaTicket(), loginReq.getCaptchaRandstr(), httpRequest);
        LoginResponse response = authService.login(loginReq);
        return RestResult.success(response, "Login success.");
    }

    /**
     * 忘记密码：发送重置链接
     */
    @PostMapping("/forgot-password")
    @RateLimit(timeWindowSeconds = 60, maxRequests = 5,
            message = "Too many reset password requests. Please try again later.")
    public RestResult<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                        HttpServletRequest httpRequest) {
        // 先进行验证码校验
        // validateCaptcha(request.getCaptchaTicket(), request.getCaptchaRandstr(), httpRequest);
        authService.forgotPassword(request);
        return RestResult.success(null, "A password reset link has been sent to your email.");
    }

    /**
     * 重置密码接口
     */
    @PostMapping("/reset-password")
    public RestResult<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return RestResult.success(null, "Password reset successful. " +
                "Please log in with your new password.");
    }

    @PostMapping("/change-password")
    public RestResult<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        authService.changePassword(request);
        return RestResult.success(null, "Password changed successfully.");
    }

    /**
     * 更新用户个人资料
     */
    @PostMapping("/user-profile")
    public RestResult<?> updateUserProfile(@Valid @RequestBody UserProfileDTO request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        userService.updateUserProfile(currentUserId, request);
        return RestResult.success("Updated", "User profile updated successfully.");
    }


    // 新增接口：分页查询当前user（可能是member可能是教练）的通知列表（业务逻辑在 service 层）
    @GetMapping("/notifications")
    public RestResult<?> getNotifications(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        // 这里我就全部返回了！
        Page<Notification> notificationsPage = notificationService.getNotificationsByUser(currentUserId, page, pageSize);
        return RestResult.success(notificationsPage, "Notifications retrieved successfully.");
    }

    // 新增接口：标记指定通知为已读（业务逻辑在 service 层）
    @PutMapping("/notifications/{notificationId}/read")
    public RestResult<?> markNotificationAsRead(@PathVariable("notificationId") Long notificationId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        notificationService.markAsRead(notificationId, currentUserId);
        return RestResult.success(null, "Notification marked as read successfully.");
    }

    // 新增删除通知接口：仅允许删除已读通知
    @DeleteMapping("/notifications/{notificationId}")
    public RestResult<?> deleteNotification(@PathVariable("notificationId") Long notificationId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        notificationService.deleteNotification(notificationId, currentUserId);
        return RestResult.success(null, "Notification deleted successfully.");
    }

    @PostMapping("/google-login")
    public RestResult<LoginResponse> googleLogin(@RequestBody @Valid GoogleLoginRequest request) {
        // 1. 直接调用 AuthService 里专门处理谷歌登录的方法
        LoginResponse loginResponse = authService.loginWithGoogle(request.getIdToken());
        // 2. 返回和普通登录一样的响应结构
        return RestResult.success(loginResponse, "Google Login success.");
    }

    // 查询所有专长常量接口
    @GetMapping("/specializations")
    public RestResult<?> listSpecializations() {
        // 调用 Service 层方法查询所有专长数据
        List<Specializations> specializations = userService.listSpecializations();
        // 使用 RestResult.success 封装返回结果，前端将收到 code、message、data 三部分
        return RestResult.success(specializations, "Specializations retrieved successfully.");
    }

    // 查询地点
    /**
     * 查询所有健身房地点
     * 前端需要的字段：
     * - title：展示名称（目前直接使用健身房的 name 字段，如有连锁信息，可在此扩展）
     * - address：标准英文地址（包含邮编）
     * - latitude：经度
     * - longitude：纬度
     * - contactInfo：联系方式
     */
    @GetMapping("/locations")
    public RestResult<?> getFitnessCentreLocations() {
        // 查询所有健身房记录，MyBatis-Plus 提供 list() 方法
        List<FitnessCentre> centres = fitnessCentreService.list();

        // 将实体转换为 VO
        List<FitnessCentreVO> voList = centres.stream().map(centre -> FitnessCentreVO.builder()
                        .title(centre.getName())  // 如有连锁需要可以改造，例如 "Chain: " + centre.getName()
                        .address(centre.getAddress())
                        .latitude(centre.getLatitude())
                        .longitude(centre.getLongitude())
                        .contactInfo(centre.getContactInfo())
                        .build())
                .collect(Collectors.toList());

        return RestResult.success(voList, "Locations retrieved successfully.");
    }
}
