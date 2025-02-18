package com.gym.controller;

import com.gym.dto.*;
import com.gym.entity.User;
import com.gym.result.RestResult;
import com.gym.service.MailService;
import com.gym.service.UserService;
import com.gym.util.JwtUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private MailService mailService;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    // BCrypt
    private PasswordEncoder passwordEncoder;

    // 用于暂存 {email -> (注册信息, 验证码)}
    private static final Map<String, PendingVerification> pendingMap = new ConcurrentHashMap<>();

    /**
     * 第一步：前端填写(邮箱+密码+其他资料), 后端发送验证码
     */
    @PostMapping("/signup")
    public RestResult<?> signup(@RequestBody SignupRequest request) {
        log.info("signup request: {}", request);

        // 1. 检查邮箱是否已被注册
        User existing = userService.getByEmail(request.getEmail());
        if (existing != null) {
            return RestResult.error("该邮箱已被注册", null);
        }

        // 2. 生成验证码并保存到pendingMap
        String code = generateRandomCode();
        PendingVerification pv = new PendingVerification();
        // pv 里面包括用户基本信息request和验证码（code）以及创建时间
        pv.setRequest(request);
        pv.setVerificationCode(code);
        pv.setCreateTime(System.currentTimeMillis());
        pendingMap.put(request.getEmail(), pv);

        // 3. 发邮件
        mailService.sendVerificationCode(request.getEmail(), code);

        return RestResult.success(null, "验证码已发送至您的邮箱，请输入验证码完成注册");
    }

    /**
     * 第二步：验证验证码，写入数据库，并通知管理员审核
     */
    @PostMapping("/verify-code")
    public RestResult<?> verifyCode(@RequestBody VerifyCodeRequest verifyReq) {
        String email = verifyReq.getEmail();
        String inputCode = verifyReq.getCode();

        PendingVerification pv = pendingMap.get(email);
        if (pv == null) {
            return RestResult.error("验证码已过期或不存在，请重新注册", null);
        }

        // 检查验证码
        if (!pv.getVerificationCode().equals(inputCode)) {
            return RestResult.error("验证码不正确，请重新输入", null);
        }

        // (可增加过期时间判断)
        // if (System.currentTimeMillis() - pv.getCreateTime() > 600_000) {
        //     return RestResult.error("验证码已过期，请重新注册", null);
        // }

        // 验证通过 -> 写入数据库
        SignupRequest req = pv.getRequest();
        User newUser = new User();
        newUser.setEmail(req.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        newUser.setName(req.getName());
        newUser.setAddress(req.getAddress());
        newUser.setDateOfBirth(req.getDateOfBirth());
        // 默认 Pending 等待管理员审核
        newUser.setAccountStatus(User.AccountStatus.Pending);
        newUser.setRole(User.Role.Member);
        newUser.setEmailVerified(true);
        userService.createUser(newUser);

        // 移除 pendingMap
        pendingMap.remove(email);

        // 发送通知给管理员
        // 这里不应该给管理员发邮件吧，有点搞笑，应该是在管理端有通知
        mailService.sendAdminNotification("admin@fitness.com",
                "新用户待审核: " + req.getEmail());

        return RestResult.success(null, "注册成功，等待管理员审核");
    }

    /**
     * 登录接口 (带JWT)
     */
    @PostMapping("/login")
    public RestResult<LoginResponse> login(@RequestBody LoginRequest loginReq) {
        User user = userService.getByEmail(loginReq.getEmail());
        if (user == null) {
            return RestResult.error("邮箱或密码错误", null);
        }
        if (!passwordEncoder.matches(loginReq.getPassword(), user.getPasswordHash())) {
            return RestResult.error("邮箱或密码错误", null);
        }
        // 判断状态
        if (user.getAccountStatus() == User.AccountStatus.Pending) {
            return RestResult.error("您的账号正等待管理员审核", null);
        }
        if (user.getAccountStatus() == User.AccountStatus.Suspended) {
            return RestResult.error("您的账号已被禁用", null);
        }

        // 生成JWT
        String token = jwtUtils.generateToken(user);
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setUserId(user.getUserID());
        resp.setRole(user.getRole());

        return RestResult.success(resp, "登录成功");
    }

    // ============== 测试接口 (保留原有) ==============
//    @PostMapping("/create")
//    public RestResult<User> createUser(@RequestBody User user) {
//        User createdUser = userService.createUser(user);
//        return RestResult.success(createdUser, "User created successfully");
//    }
//
//    @GetMapping("/list")
//    public RestResult<List<User>> listUsers() {
//        List<User> users = userService.getAllUsers();
//        return RestResult.success(users, "Fetched user list successfully");
//    }

    // ============== 内部方法&DTO ==============

    private String generateRandomCode() {
        int code = (int)((Math.random() * 9 + 1) * 100000);
        return String.valueOf(code);
    }

    // 这里的 DTO放到单独的文件中了
//    @Data
//    static class PendingVerification {
//        private SignupRequest request;
//        private String verificationCode;
//        private long createTime;
//    }
//
//    @Data
//    public static class SignupRequest {
//        private String email;
//        private String password;
//        private String name;
//        private String address;
//        private Date dateOfBirth;
//    }
//
//    @Data
//    public static class VerifyCodeRequest {
//        private String email;
//        private String code;
//    }
//
//    @Data
//    public static class LoginRequest {
//        private String email;
//        private String password;
//    }
//
//    @Data
//    public static class LoginResponse {
//        private String token;
//        private Long userId;
//        private User.Role role;
//    }
}
