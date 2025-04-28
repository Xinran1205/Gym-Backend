package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.dao.TrainerProfileDao;
import com.gym.dao.UserDao;
import com.gym.dto.TrainerProfileQuery;
import com.gym.entity.TrainerConnectRequest;
import com.gym.entity.TrainerProfile;
import com.gym.entity.User;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.service.TrainerConnectRequestService;
import com.gym.service.TrainerProfileService;
import com.gym.service.UserService;
import com.gym.util.SecurityUtils;
import com.gym.vo.TrainerAllProfile;
import com.gym.vo.TrainerProfileVO;
import com.gym.vo.UserProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TrainerProfileServiceImpl extends ServiceImpl<TrainerProfileDao, TrainerProfile>
        implements TrainerProfileService {

    @Autowired
    UserService userService;

//    @Autowired
//    private TrainerConnectRequestService trainerConnectRequestService;

    @Autowired
    private TrainerProfileDao trainerProfileDao;

    /**
     * 为指定的教练用户创建一条默认的 TrainerProfile 记录。
     * 默认值均为空（或 0），后续教练可以通过完善页面进行修改。
     *
     * @param userId 教练用户的ID
     */
    @Override
    public void createDefaultTrainerProfile(Long userId, String name) {
        // 使用builder
        TrainerProfile trainerProfile = TrainerProfile.builder()
                .userId(userId)
                .name(name)
                .certifications("")
                .specializations("")
                .yearsOfExperience(0)
                .biography("")
                .workplace("")
                .build();
        // 保存记录到数据库
        this.save(trainerProfile);
    }

    // 查询教练的profile和自己的简单信息
    @Override
    public TrainerAllProfile getTrainerAllProfile(Long currentUserId){
        // 根据当前用户ID查询 User 表中的记录
        // 不要的字段不要查询出来
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(User::getName, User::getDateOfBirth, User::getAddress, User::getEmail)
                .eq(User::getUserID, currentUserId);
        User user = userService.getOne(queryWrapper);

        if (user == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "User not found.");
        }
        // 根据当前用户ID查询 TrainerProfile 表中的记录,用户id是profile表的外键
        LambdaQueryWrapper<TrainerProfile> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.eq(TrainerProfile::getUserId, currentUserId);
        TrainerProfile trainerProfile = this.getOne(queryWrapper2);
        if (trainerProfile == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Trainer profile not found.");
        }

        // 使用build
        return TrainerAllProfile.builder()
                .trainerProfile(trainerProfile)
                .dateOfBirth(user.getDateOfBirth())// 设置 TrainerProfile 对象
                .address(user.getAddress())
                .email(user.getEmail())
                .build();                               // 构建最终对象
    }

    @Override
    public Page<TrainerProfileVO> listTrainers(TrainerProfileQuery query) {
        // 获取当前 member 的ID（当前登录用户）
        Long currentMemberId = SecurityUtils.getCurrentUserId();
        if (currentMemberId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }

        // 原理是利用sql语句的limit和offset
        // 1. 构造分页对象（默认第1页，每页10条，可根据需求做校验/限制）
        Page<TrainerProfile> page = new Page<>(query.getPage(), query.getPageSize());
        // 使用自定义 SQL 进行 LEFT JOIN 查询
        Page<TrainerProfileVO> voPage = trainerProfileDao.selectTrainersWithConnectStatus(page, query, currentMemberId);
        return voPage;

//        // 2. 构造查询条件
//        LambdaQueryWrapper<TrainerProfile> wrapper = new LambdaQueryWrapper<>();
//
//        // 这两个字段我已经加上了索引！！
//        // 这里是精确匹配
//        if (StringUtils.hasText(query.getSpecializations())) {
//            wrapper.eq(TrainerProfile::getSpecializations, query.getSpecializations());
//        }
//        if (StringUtils.hasText(query.getWorkplace())) {
//            wrapper.eq(TrainerProfile::getWorkplace, query.getWorkplace());
//        }
//
//        // 3. 使用 MyBatis-Plus 执行分页查询
//        Page<TrainerProfile> profilePage = this.page(page, wrapper);
//
//        // 4. 获取当前 member 的ID（当前登录用户）
//        Long currentMemberId = SecurityUtils.getCurrentUserId();
//        if (currentMemberId == null) {
//            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
//        }
//
//        // 5. 从查询结果中提取所有教练的 userId 列表
//        List<Long> trainerIds = profilePage.getRecords().stream()
//                .map(TrainerProfile::getUserId)
//                .collect(Collectors.toList());
//
//        // 6. 查询当前 member 对这些教练的连接申请记录（单次查询，避免 N+1 问题）
//        LambdaQueryWrapper<TrainerConnectRequest> connectWrapper = new LambdaQueryWrapper<>();
//        connectWrapper.eq(TrainerConnectRequest::getMemberId, currentMemberId)
//                .in(TrainerConnectRequest::getTrainerId, trainerIds);
//        List<TrainerConnectRequest> connectRequests = trainerConnectRequestService.list(connectWrapper);
//
//        // 将查询到的连接记录转换为 Map，key 为 trainerId
//        Map<Long, TrainerConnectRequest> connectMap = connectRequests.stream()
//                .collect(Collectors.toMap(TrainerConnectRequest::getTrainerId, Function.identity()));
//
//        // 7. 转换 TrainerProfile 到 TrainerProfileVO，同时设置连接状态
//        Page<TrainerProfileVO> voPage = new Page<>(profilePage.getCurrent(),
//                profilePage.getSize(),
//                profilePage.getTotal());
//        List<TrainerProfileVO> voList = profilePage.getRecords().stream()
//                .map(trainerProfile -> {
//                    TrainerProfileVO vo = new TrainerProfileVO();
//                    // 拷贝基本属性
//                    BeanUtils.copyProperties(trainerProfile, vo);
//                    vo.setUserId(trainerProfile.getUserId());
//                    // 根据当前 member 与该教练是否有连接申请，设置连接状态
//                    TrainerConnectRequest connectRequest = connectMap.get(trainerProfile.getUserId());
//                    if (connectRequest != null) {
//                        vo.setConnectStatus(connectRequest.getStatus());
//                    } else {
//                        // 如果没有连接记录，设置为 null
//                        vo.setConnectStatus(null);
//                    }
//                    return vo;
//                })
//                .collect(Collectors.toList());
//        voPage.setRecords(voList);
//
//        return voPage;
    }

}

