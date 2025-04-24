package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.bloomFilter.BloomFilterUtil;
import com.gym.dao.SpecializationsDao;
import com.gym.dao.UserDao;
import com.gym.dto.PendingVerification;
import com.gym.dto.SignupRequest;
import com.gym.dto.UserProfileDTO;
import com.gym.dto.VerifyCodeRequest;
import com.gym.entity.Specializations;
import com.gym.entity.User;
import com.gym.enumeration.ErrorCode;
import com.gym.event.UserCreatedEvent;
import com.gym.exception.CustomException;
import com.gym.service.MailService;
import com.gym.service.UserService;
import com.gym.vo.TrainerBasicInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserDao, User> implements UserService {

    @Autowired
    private MailService mailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private RedisCacheServiceImpl redisCacheService;

    @Autowired
    private BloomFilterUtil bloomFilterUtil;

    @Autowired
    private SpecializationsDao Specializationsdao;

    @Override
    public void sendSignupVerification(SignupRequest signupRequest) {
        // 1. 校验角色，禁止注册管理员账号
        if (signupRequest.getRole() == User.Role.admin) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "You do not have permission to register as an admin.");
        }
        // 2. 检查邮箱是否已注册
        User existing = this.getByEmail(signupRequest.getEmail());
        if (existing != null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Email is already registered.");
        }
        // 3. 生成验证码
        String code = generateRandomCode();
        PendingVerification pv = new PendingVerification();
        pv.setRequest(signupRequest);
        pv.setVerificationCode(code);
        pv.setCreateTime(System.currentTimeMillis());
        // 4. 保存到 Redis（5分钟有效）
        String redisKey = "SIGNUP_PENDING_" + signupRequest.getEmail();
        // String 类型来存储对象，只要你的 RedisTemplate 配置了合适的序列化器
        // （例如 JSON 序列化器），这样对象就会被序列化成字符串存储。
        redisTemplate.opsForValue().set(redisKey, pv, 5, TimeUnit.MINUTES);
        // 5. 发送验证码邮件
        mailService.sendVerificationCode(signupRequest.getEmail(), code);
    }

    @Override
    public void verifySignupCode(VerifyCodeRequest verifyReq) {
        String email = verifyReq.getEmail();
        String inputCode = verifyReq.getCode();
        String redisKey = "SIGNUP_PENDING_" + email;
        PendingVerification pv = (PendingVerification) redisTemplate.opsForValue().get(redisKey);
        if (pv == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Verification code expired or not found. Please sign up again.");
        }
        if (!pv.getVerificationCode().equals(inputCode)) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Invalid verification code. Please try again.");
        }
        // 将注册请求转换为 User 实体并写入数据库
        SignupRequest req = pv.getRequest();
        User newUser = convertSignupRequestToUser(req);
        createUser(newUser);
        // 事件发布，通知监听器，如果是 Trainer 则创建 TrainerProfile
        eventPublisher.publishEvent(new UserCreatedEvent(this, newUser));

        // 清理 Redis 中的 PendingVerification 信息
        redisTemplate.delete(redisKey);
        // 更新缓存和布隆过滤器
        // 感觉不需要更新缓存，直接更新布隆过滤器
        redisCacheService.updateUser(newUser);
        bloomFilterUtil.addUserToBloomFilter(newUser.getUserID());
    }

    @Override
    public void updateUserProfile(Long userId, UserProfileDTO userProfileDTO) {
        // 使用 MyBatis Plus 的 LambdaUpdateWrapper 封装更新逻辑
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUserID, userId)
                .set(User::getName, userProfileDTO.getName())
                .set(User::getAddress, userProfileDTO.getAddress())
                .set(User::getDateOfBirth, userProfileDTO.getDateOfBirth());
        boolean updateResult = this.update(updateWrapper);
        if (!updateResult) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Update failed: User profile may not exist.");
        }
        log.info("User [{}] profile updated successfully", userId);
    }


    @Override
    public List<TrainerBasicInfoVO> listOtherTrainersBasicInfo(Long excludeTrainerId) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role", User.Role.trainer)                       // 只要是 trainer
                .ne(excludeTrainerId != null, "user_id", excludeTrainerId) // 排除自己
                .select("user_id", "name");                          // 仅查所需列

        List<User> trainers = baseMapper.selectList(queryWrapper);

        return trainers.stream()
                .map(u -> TrainerBasicInfoVO.builder()
                        .trainerId(u.getUserID())
                        .name(u.getName())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Page<User> getPendingUsers(Page<User> page) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("account_status", User.AccountStatus.Pending)
                .orderByAsc("created_at");              // oldest first
        return this.page(page, wrapper);
    }



    // 将 SignupRequest 转换为 User 实体
    private User convertSignupRequestToUser(SignupRequest req) {
        User newUser = new User();
        newUser.setEmail(req.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        newUser.setName(req.getName());
        newUser.setAddress(req.getAddress());
        newUser.setDateOfBirth(req.getDateOfBirth());
        newUser.setAccountStatus(User.AccountStatus.Pending);
        newUser.setRole(req.getRole());
        return newUser;
    }

    // 生成六位随机验证码
    private String generateRandomCode() {
        int code = (int) ((Math.random() * 9 + 1) * 100000);
        return String.valueOf(code);
    }

    @Override
    public List<Specializations> listSpecializations() {
        // 使用 MyBatis-Plus 提供的 selectList(null) 查询所有记录
        return Specializationsdao.selectList(null);
    }

    @Override
    public User createUser(User user) {
        baseMapper.insert(user);
        return user;
    }

    @Override
    public User getUserById(Long userID) {
        return baseMapper.selectById(userID);
    }

    @Override
    public User getByEmail(String email) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public Page<User> listUsers(int page, int pageSize, String role) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();

        if (role != null) {
            try {
                User.Role r = User.Role.valueOf(role);
                if (r == User.Role.admin) {
                    throw new IllegalArgumentException("Cannot query admin here");
                }
                qw.eq(User::getRole, r);
            } catch (IllegalArgumentException ex) {
                throw new CustomException(ErrorCode.BAD_REQUEST,
                        "Invalid role parameter: must be 'member' or 'trainer'");
            }
        } else {
            // 默认查询 member + trainer
            qw.in(User::getRole, User.Role.member, User.Role.trainer);
        }

        // 按创建时间升序
        qw.orderByAsc(User::getCreateTime);

        Page<User> pageObj = new Page<>(page, pageSize);
        return this.page(pageObj, qw);
    }
}
