package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gym.dto.ForgotPasswordRequest;
import com.gym.dto.LoginRequest;
import com.gym.dto.ResetPasswordRequest;
import com.gym.entity.User;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.service.AuthService;
import com.gym.service.MailService;
import com.gym.service.UserService;
import com.gym.util.JwtUtils;
import com.gym.vo.LoginResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MailService mailService;

    @Autowired
    private RedisCacheServiceImpl redisCacheService;

    @Override
    public LoginResponse login(LoginRequest loginReq) {
        User user = userService.getByEmail(loginReq.getEmail());
        if (user == null || !passwordEncoder.matches(loginReq.getPassword(), user.getPasswordHash())) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Incorrect email or password.");
        }
        if (user.getAccountStatus() == User.AccountStatus.Pending) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Your account is pending admin review.");
        }
        if (user.getAccountStatus() == User.AccountStatus.Suspended) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Your account is suspended.");
        }
        // 生成JWT
        String token = jwtUtils.generateToken(user);
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setUserId(user.getUserID());
        resp.setRole(user.getRole());
        return resp;
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        // 只查出必要的字段
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                        User::getUserID,
                        User::getEmail,
                        User::getAccountStatus
                )
                .eq(User::getEmail, request.getEmail());
        User user = userService.getOne(queryWrapper);

//
//        User user = userService.getByEmail(request.getEmail());
        // 为安全考虑，如果用户不存在，则直接返回成功提示
        if (user == null) {
            return;
        }
        if (user.getAccountStatus() == User.AccountStatus.Suspended || user.getAccountStatus() == User.AccountStatus.Pending) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Your account is not eligible for password reset.");
        }
        // 生成重置密码的 JWT Token
        String resetToken = jwtUtils.generateResetToken(user);
        // 构造重置链接（前端路由地址自行配置）
        String resetLink = "https://yourapp.com/reset-password?token=" + resetToken;
        // 异步发送重置密码邮件
        mailService.sendResetLink(request.getEmail(), resetLink);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        // 这个地方就这么写吧，因为考虑到缓存！！！
        // 通过JWT验证 token，获得邮箱
        String email = jwtUtils.verifyResetToken(request.getToken());
        User user = userService.getByEmail(email);
        if (user == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Email is not registered.");
        }
        // 更新密码并同步缓存
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userService.updateById(user);
        redisCacheService.updateUser(user);
        log.info("User password reset success, email={}", email);
    }
}

