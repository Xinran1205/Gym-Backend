package com.gym.controller;

import com.gym.AOP.RateLimit;
import com.gym.bloomFilter.BloomFilterUtil;
import com.gym.dto.*;
import com.gym.dto.redis.PendingPasswordReset;
import com.gym.service.impl.RedisCacheServiceImpl;
import com.gym.util.IpUtil;
import com.gym.util.TencentCaptchaUtil;
import com.gym.vo.LoginResponse;
import com.gym.entity.User;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.result.RestResult;
import com.gym.service.MailService;
import com.gym.service.UserService;
import com.gym.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.concurrent.TimeUnit;

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BloomFilterUtil bloomFilterUtil;

    @Autowired
    private RedisCacheServiceImpl redisCacheService;

    @Autowired
    private IpUtil ipUtil;

    /**
     * 新增：RedisTemplate，用于存储 PendingVerification
     */
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private TencentCaptchaUtil tencentCaptchaUtil;

    /**
     * 第一步：前端填写(邮箱+密码+其他资料)，后端发送验证码
     */
    @PostMapping("/signup")
    // 限流注解，60 秒内最多 5 次请求
    // TODO 测试限流是否正确
    @RateLimit(timeWindowSeconds = 60, maxRequests = 5,
            message = "Too many signup requests. Please try again later.")
    public RestResult<?> signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        log.info("signup request: {}", request);


        // 在这里加上验证码拼图校验，如果验证码不正确，不往下执行
        // 1. 调用腾讯验证码接口进行校验
        // 使用工具函数获取真实的客户端IP
//        String clientIp = ipUtil.getClientIp(httpRequest);
//
//        boolean captchaValid = tencentCaptchaUtil.
//                verifyCaptcha(request.getCaptchaTicket(), request.getCaptchaRandstr(), clientIp);
//        if (!captchaValid) {
//            throw new CustomException(ErrorCode.BAD_REQUEST, "Captcha verification failed.");
//        }

        // 2. 检查邮箱是否已被注册
        User existing = userService.getByEmail(request.getEmail());
        if (existing != null) {
            // 抛出异常 -> 全局异常处理器返回英文提示
            throw new CustomException(ErrorCode.BAD_REQUEST, "Email is already registered.");
        }

        // 3. 生成验证码并创建 PendingVerification 对象
        String code = generateRandomCode();
        PendingVerification pv = new PendingVerification();
        // 封装用户注册请求和验证码信息
        pv.setRequest(request);
        pv.setVerificationCode(code);
        pv.setCreateTime(System.currentTimeMillis());

        // 4. 保存到 Redis，设置一个 5 分钟的过期时间，同理验证码就是 5 分钟有效
        String redisKey = "SIGNUP_PENDING_" + request.getEmail();
        redisTemplate.opsForValue().set(redisKey, pv, 5, TimeUnit.MINUTES);

        // 5. 发送邮件
        mailService.sendVerificationCode(request.getEmail(), code);

        return RestResult.success(null, "Verification code has been sent to your email. Please enter it to complete registration.");
    }

    /**
     * 第二步：验证验证码，写入数据库，并通知管理员审核
     */
    @PostMapping("/verify-code")
    public RestResult<?> verifyCode(@Valid  @RequestBody VerifyCodeRequest verifyReq) {
        String email = verifyReq.getEmail();
        String inputCode = verifyReq.getCode();

        String redisKey = "SIGNUP_PENDING_" + email;
        // 从Redis里拿到 PendingVerification
        // 不需要考虑这个redis穿透问题，不存在就是过期了，不会查询数据库！！！
        PendingVerification pv = (PendingVerification) redisTemplate.opsForValue().get(redisKey);
        if (pv == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Verification code expired or not found. Please sign up again.");
        }

        // 检查验证码是否正确
        if (!pv.getVerificationCode().equals(inputCode)) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Invalid verification code. Please try again.");
        }

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

        // 验证完成后，从Redis移除,这个是防止重复注册！
        redisTemplate.delete(redisKey);

        // 添加到redis
        redisCacheService.updateUser(newUser);

        // 添加到布隆过滤器
        bloomFilterUtil.addUserToBloomFilter(newUser.getUserID());

        return RestResult.success(null, "Registration successful. Awaiting admin approval.");
    }

    /**
     * 登录接口 (带JWT)
     */
    @PostMapping("/login")
    public RestResult<LoginResponse> login(@Valid @RequestBody LoginRequest loginReq) {
        User user = userService.getByEmail(loginReq.getEmail());
        if (user == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Incorrect email or password.");
        }
        // 校验密码
        if (!passwordEncoder.matches(loginReq.getPassword(), user.getPasswordHash())) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Incorrect email or password.");
        }
        // 判断账户状态
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

        return RestResult.success(resp, "Login success.");
    }


    @PostMapping("/forgot-password")
    @RateLimit(timeWindowSeconds = 60, maxRequests = 5,
            message = "Too many reset password requests. Please try again later.")
    public RestResult<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("forgotPassword request: {}", request);

        User user = userService.getByEmail(request.getEmail());
        if (user == null) {
            // 出于安全原因，统一返回成功信息（或提示已发送）
            return RestResult.success(null, "If the email is registered, a reset link has been sent.");
        }

        // 生成重置密码的 JWT Token
        String resetToken = jwtUtils.generateResetToken(user);

        // 生成重置链接（前端路由地址需自行配置）
        String resetLink = "https://yourapp.com/reset-password?token=" + resetToken;

        // 异步发送邮件
        mailService.sendResetLink(request.getEmail(), resetLink);

        return RestResult.success(null, "A password reset link has been sent to your email.");
    }

    /**
     * 忘记密码：验证重置验证码 & 重置密码
     */
    @PostMapping("/reset-password")
    public RestResult<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("resetPassword request with token: {}", request.getToken());

        // 通过 JWT 验证 token，有效则返回邮箱
        String email = jwtUtils.verifyResetToken(request.getToken());

        User user = userService.getByEmail(email);
        if (user == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Email is not registered.");
        }

        // 更新密码（使用 bcrypt 加密）
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userService.updateById(user);

        // 同步更新缓存
        redisCacheService.updateUser(user);

        log.info("User password reset success, email={}", email);
        return RestResult.success(null, "Password reset successful. Please log in with your new password.");
    }



    // ============== 内部工具方法 ==============
    private String generateRandomCode() {
        // 生成六位随机数的简单示例
        int code = (int)((Math.random() * 9 + 1) * 100000);
        return String.valueOf(code);
    }
}








//package com.gym.controller;
//
//import com.gym.dto.*;
//import com.gym.entity.User;
//import com.gym.result.RestResult;
//import com.gym.service.MailService;
//import com.gym.service.UserService;
//import com.gym.util.JwtUtils;
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//@RestController
//@RequestMapping("/user")
//@Slf4j
//public class UserController {
//
//    @Autowired
//    private UserService userService;
//    @Autowired
//    private MailService mailService;
//    @Autowired
//    private JwtUtils jwtUtils;
//    @Autowired
//    // BCrypt
//    private PasswordEncoder passwordEncoder;
//
//    // 用于暂存 {email -> (注册信息, 验证码)}
//    private static final Map<String, PendingVerification> pendingMap = new ConcurrentHashMap<>();
//
//    /**
//     * 第一步：前端填写(邮箱+密码+其他资料), 后端发送验证码
//     */
//    @PostMapping("/signup")
//    public RestResult<?> signup(@RequestBody SignupRequest request) {
//        log.info("signup request: {}", request);
//
//        // 1. 检查邮箱是否已被注册
//        User existing = userService.getByEmail(request.getEmail());
//        if (existing != null) {
//            return RestResult.error("该邮箱已被注册", null);
//        }
//
//        // 2. 生成验证码并保存到pendingMap
//        String code = generateRandomCode();
//        PendingVerification pv = new PendingVerification();
//        // pv 里面包括用户基本信息request和验证码（code）以及创建时间
//        pv.setRequest(request);
//        pv.setVerificationCode(code);
//        pv.setCreateTime(System.currentTimeMillis());
//        pendingMap.put(request.getEmail(), pv);
//
//        // 3. 发邮件
//        mailService.sendVerificationCode(request.getEmail(), code);
//
//        return RestResult.success(null, "验证码已发送至您的邮箱，请输入验证码完成注册");
//    }
//
//    /**
//     * 第二步：验证验证码，写入数据库，并通知管理员审核
//     */
//    @PostMapping("/verify-code")
//    public RestResult<?> verifyCode(@RequestBody VerifyCodeRequest verifyReq) {
//        String email = verifyReq.getEmail();
//        String inputCode = verifyReq.getCode();
//
//        PendingVerification pv = pendingMap.get(email);
//        if (pv == null) {
//            return RestResult.error("验证码已过期或不存在，请重新注册", null);
//        }
//
//        // 检查验证码
//        if (!pv.getVerificationCode().equals(inputCode)) {
//            return RestResult.error("验证码不正确，请重新输入", null);
//        }
//
//        // (可增加过期时间判断)
//        // if (System.currentTimeMillis() - pv.getCreateTime() > 600_000) {
//        //     return RestResult.error("验证码已过期，请重新注册", null);
//        // }
//
//        // 验证通过 -> 写入数据库
//        SignupRequest req = pv.getRequest();
//        User newUser = new User();
//        newUser.setEmail(req.getEmail());
//        newUser.setPasswordHash(passwordEncoder.encode(req.getPassword()));
//        newUser.setName(req.getName());
//        newUser.setAddress(req.getAddress());
//        newUser.setDateOfBirth(req.getDateOfBirth());
//        // 默认 Pending 等待管理员审核
//        newUser.setAccountStatus(User.AccountStatus.Pending);
//        newUser.setRole(User.Role.Member);
//        newUser.setEmailVerified(true);
//        userService.createUser(newUser);
//
//        // 移除 pendingMap
//        pendingMap.remove(email);
//
//        // 发送通知给管理员
//        // 这里不应该给管理员发邮件吧，有点搞笑，应该是在管理端有通知
//        mailService.sendAdminNotification("admin@fitness.com",
//                "新用户待审核: " + req.getEmail());
//
//        return RestResult.success(null, "注册成功，等待管理员审核");
//    }
//
//    /**
//     * 登录接口 (带JWT)
//     */
//    @PostMapping("/login")
//    public RestResult<LoginResponse> login(@RequestBody LoginRequest loginReq) {
//        User user = userService.getByEmail(loginReq.getEmail());
//        if (user == null) {
//            return RestResult.error("邮箱或密码错误", null);
//        }
//        if (!passwordEncoder.matches(loginReq.getPassword(), user.getPasswordHash())) {
//            return RestResult.error("邮箱或密码错误", null);
//        }
//        // 判断状态
//        if (user.getAccountStatus() == User.AccountStatus.Pending) {
//            return RestResult.error("您的账号正等待管理员审核", null);
//        }
//        if (user.getAccountStatus() == User.AccountStatus.Suspended) {
//            return RestResult.error("您的账号已被禁用", null);
//        }
//
//        // 生成JWT
//        String token = jwtUtils.generateToken(user);
//        LoginResponse resp = new LoginResponse();
//        resp.setToken(token);
//        resp.setUserId(user.getUserID());
//        resp.setRole(user.getRole());
//
//        return RestResult.success(resp, "登录成功");
//    }
//    // ============== 内部方法&DTO ==============
//
//    private String generateRandomCode() {
//        int code = (int)((Math.random() * 9 + 1) * 100000);
//        return String.valueOf(code);
//    }
//
//    // 这里的 DTO放到单独的文件中了
////    @Data
////    static class PendingVerification {
////        private SignupRequest request;
////        private String verificationCode;
////        private long createTime;
////    }
////
////    @Data
////    public static class SignupRequest {
////        private String email;
////        private String password;
////        private String name;
////        private String address;
////        private Date dateOfBirth;
////    }
////
////    @Data
////    public static class VerifyCodeRequest {
////        private String email;
////        private String code;
////    }
////
////    @Data
////    public static class LoginRequest {
////        private String email;
////        private String password;
////    }
////
////    @Data
////    public static class LoginResponse {
////        private String token;
////        private Long userId;
////        private User.Role role;
////    }
//}
