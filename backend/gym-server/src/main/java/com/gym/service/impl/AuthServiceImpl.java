package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.gym.dto.ChangePasswordRequest;
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
import com.gym.util.SecurityUtils;
import com.gym.vo.LoginResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;


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

    @Value("${google.clientId}")
    private String googleClientId;

    /**
     * 使用谷歌 ID Token 登录
     * @param googleIdToken 前端传来的谷歌ID Token
     * @return LoginResponse
     */
    @Override
    public LoginResponse loginWithGoogle(String googleIdToken) {
        // 1. 验证并解析 Google ID Token
        GoogleIdToken.Payload payload = verifyGoogleIdToken(googleIdToken);
        // 如果验证失败，verifyGoogleIdToken 会抛异常或返回 null，视你实现而定
        if (payload == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Invalid Google ID Token.");
        }

        // 2. 从 payload 中获取关键信息： email、sub(谷歌唯一标识)、name 等
        String email = payload.getEmail();
        String googleUserId = payload.getSubject(); // sub
        String name = (String) payload.get("name");

        log.info("Google user info: email={}, sub={}, name={}", email, googleUserId, name);

        // 3. 在数据库中查找是否有对应用户
        User user = userService.getByEmail(email);
        if (user == null) {
            // 如果用户不存在，则先自动注册一个新账号
            user = registerNewGoogleUser(email, name);
            LoginResponse resp = new LoginResponse();
            return resp;

        } else {
            // 如果用户已存在，你可以选择更新用户信息，比如更新 name 等
            // user.setName(name);
            // userService.updateById(user);
        }

        // 4. 检查用户状态（如 Pending、Suspended）
        if (user.getAccountStatus() == User.AccountStatus.Pending) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Your account is pending admin review.");
        }
        if (user.getAccountStatus() == User.AccountStatus.Suspended) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Your account is suspended.");
        }

        // 5. 生成系统自己的 JWT
        String token = jwtUtils.generateToken(user);
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setUserId(user.getUserID());
        resp.setRole(user.getRole());

        return resp;
    }

    /**
     * 使用 Google 提供的库验证 ID Token
     */
    private GoogleIdToken.Payload verifyGoogleIdToken(String idTokenString) {
        try {
            // 构造一个 verifier
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                return idToken.getPayload();
            } else {
                log.error("Invalid ID Token, verify returned null.");
                return null;
            }
        } catch (Exception e) {
            log.error("Google ID Token verify exception: ", e);
            return null;
        }
    }

    /**
     * 针对全新的 Google 用户，自动生成一个账号并写入数据库
     */
    private User registerNewGoogleUser(String email, String name) {
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(name);
        // 如果你允许后续用账号密码登录，可以设置一个随机密码，或留空
        // 这里只做个示例
//        newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        newUser.setRole(User.Role.member);
        newUser.setAccountStatus(User.AccountStatus.Pending);
        // 也可以直接设为 Approved 看你业务需要

        userService.createUser(newUser);  // 你已有的创建逻辑

        // 如果需要异步创建其他信息，也可以在这里发布事件或做后续操作
        return newUser;
    }


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
        String resetLink = "http://localhost:5173/reset-password?token=" + resetToken;
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

    @Override
    public void changePassword(ChangePasswordRequest request) {
        // 获取当前登录用户的ID
        Long currentUserId = SecurityUtils.getCurrentUserId();
        // 根据用户ID查询用户信息
        User user = userService.getById(currentUserId);
        if (user == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "User not found.");
        }
        // 校验用户输入的旧密码是否正确
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Old password is incorrect.");
        }
        // 更新新密码（先加密再存储）
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        // 更新数据库
        boolean updateSuccess = userService.updateById(user);
        if (!updateSuccess) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to update password. Please try again.");
        }
    }
}

