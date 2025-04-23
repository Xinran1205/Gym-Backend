package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.dao.TrainerConnectRequestDao;
import com.gym.dto.TrainerConnectDecisionDTO;
import com.gym.dto.TrainerConnectRequestDTO;
import com.gym.entity.Notification;
import com.gym.entity.TrainerConnectRequest;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.service.NotificationService;
import com.gym.service.TrainerConnectRequestService;
import com.gym.service.UserService;
import com.gym.vo.ConnectedMemberVO;
import com.gym.vo.PendingConnectRequestVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.gym.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;

@Service
@Slf4j
public class TrainerConnectRequestServiceImpl extends ServiceImpl<TrainerConnectRequestDao, TrainerConnectRequest>
        implements TrainerConnectRequestService {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;   // new dependency

    // 判断当前 member 是否已和指定 trainer 建立连接
    @Override
    public boolean isConnected(Long memberId, Long trainerId) {
        LambdaQueryWrapper<TrainerConnectRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrainerConnectRequest::getMemberId, memberId)
                .eq(TrainerConnectRequest::getTrainerId, trainerId)
                .eq(TrainerConnectRequest::getStatus, TrainerConnectRequest.RequestStatus.Accepted);
        return this.count(queryWrapper) > 0;
    }

    // 统计指定 member 当前待审核（Pending）状态的连接申请数量
    @Override
    public int countPendingRequests(Long memberId) {
        LambdaQueryWrapper<TrainerConnectRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrainerConnectRequest::getMemberId, memberId)
                .eq(TrainerConnectRequest::getStatus, TrainerConnectRequest.RequestStatus.Pending);
        return this.count(queryWrapper);
    }

    // member提交连接申请
    @Override
    public void submitConnectRequest(TrainerConnectRequestDTO dto, Long memberId) {
        TrainerConnectRequest request = TrainerConnectRequest.builder()
                .memberId(memberId)
                .trainerId(dto.getTrainerId())
                .status(TrainerConnectRequest.RequestStatus.Pending)
                .requestMessage(dto.getRequestMessage())
                .build();
        this.save(request);
        // 生成并发送通知给教练
        Notification notification = Notification.builder()
                .userId(dto.getTrainerId())
                .title("New connection request")
                // 你有一个新的连接申请
                .message("You have a new connection request.")
                .type(Notification.NotificationType.INFO)
                .isRead(false)
                .build();
        notificationService.sendNotification(notification);
    }


    // Trainer 接受连接申请
    @Override
    public void acceptConnectRequest(TrainerConnectDecisionDTO dto, Long trainerId) {
        TrainerConnectRequest request = this.getById(dto.getRequestId());
        if (request == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Connect request not found.");
        }
        // 确保该申请属于当前教练
        if (!request.getTrainerId().equals(trainerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not authorized to process this request.");
        }
        // 申请必须处于待审核状态
        if (request.getStatus() != TrainerConnectRequest.RequestStatus.Pending) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "This connect request has already been processed.");
        }
        // 更新状态和反馈信息
        request.setStatus(TrainerConnectRequest.RequestStatus.Accepted);
        request.setResponseMessage(dto.getResponseMessage());
        boolean updateResult = this.updateById(request);
        if (!updateResult) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Unable to accept connect request.");
        }
        log.info("Trainer [{}] accepted connect request [{}]", trainerId, dto.getRequestId());

        // 生成并发送通知给申请的 member
        Notification notification = Notification.builder()
                .userId(request.getMemberId())
                .title("Application result notification")
                // 你的其中一个教练连接申请已被接受
                .message("One of your trainer connection requests has been accepted.")
                .type(Notification.NotificationType.INFO)
                .isRead(false)
                .build();
        notificationService.sendNotification(notification);
    }

    // Trainer 拒绝连接申请
    @Override
    public void rejectConnectRequest(TrainerConnectDecisionDTO dto, Long trainerId) {
        TrainerConnectRequest request = this.getById(dto.getRequestId());
        if (request == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Connect request not found.");
        }
        // 确保该申请属于当前教练
        if (!request.getTrainerId().equals(trainerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not authorized to process this request.");
        }
        // 申请必须处于待审核状态
        if (request.getStatus() != TrainerConnectRequest.RequestStatus.Pending) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "This connect request has already been processed.");
        }
        // 更新状态和反馈信息
        request.setStatus(TrainerConnectRequest.RequestStatus.Rejected);
        request.setResponseMessage(dto.getResponseMessage());
        boolean updateResult = this.updateById(request);
        if (!updateResult) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Unable to reject connect request.");
        }
        log.info("Trainer [{}] rejected connect request [{}]", trainerId, dto.getRequestId());

        // 生成并发送通知给申请的 member
        Notification notification = Notification.builder()
                .userId(request.getMemberId())
                .title("Application result notification")
                .message("One of your trainer connection requests has been rejected.")
                .type(Notification.NotificationType.INFO)
                .isRead(false)
                .build();
        notificationService.sendNotification(notification);
    }

    @Override
    public List<PendingConnectRequestVO> getPendingConnectRequestsForTrainer(Long trainerId) {

        // (1) fetch raw pending requests
        LambdaQueryWrapper<TrainerConnectRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainerConnectRequest::getTrainerId, trainerId)
                .eq(TrainerConnectRequest::getStatus, TrainerConnectRequest.RequestStatus.Pending)
                .orderByAsc(TrainerConnectRequest::getCreateTime);

        List<TrainerConnectRequest> requests = this.list(wrapper);
        if (requests.isEmpty()) {
            return new ArrayList<>();
        }

        // (2) batch load member names
        Set<Long> memberIds = requests.stream()
                .map(TrainerConnectRequest::getMemberId)
                .collect(Collectors.toSet());

        Map<Long, String> nameMap = userService.listByIds(memberIds).stream()
                .collect(Collectors.toMap(User::getUserID, User::getName));

        // (3) map to VO
        return requests.stream()
                .map(r -> PendingConnectRequestVO.builder()
                        .requestId(r.getRequestId())
                        .memberId(r.getMemberId())
                        .memberName(nameMap.getOrDefault(r.getMemberId(), "Unknown"))
                        .requestMessage(r.getRequestMessage())
                        .createdAt(r.getCreateTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<ConnectedMemberVO> listConnectedMembers(Long trainerId) {
        // 1. 查出所有已 Accepted 的连接记录（包括 createdAt）
        LambdaQueryWrapper<TrainerConnectRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainerConnectRequest::getTrainerId, trainerId)
                .eq(TrainerConnectRequest::getStatus, TrainerConnectRequest.RequestStatus.Accepted)
                .orderByAsc(TrainerConnectRequest::getCreateTime);
        List<TrainerConnectRequest> accepted = this.list(wrapper);
        if (accepted.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 去重并收集 memberId
        LinkedHashSet<Long> memberIds = accepted.stream()
                .map(TrainerConnectRequest::getMemberId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 3. 批量拉取会员 User 实体
        List<User> users = userService.listByIds(memberIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getUserID, u -> u));

        // 4. 构造 VO 列表
        List<ConnectedMemberVO> result = new ArrayList<>();
        for (Long memberId : memberIds) {
            User u = userMap.get(memberId);
            String name  = (u != null ? u.getName() : "Unknown");
            String email = (u != null ? u.getEmail() : null);
            // 找到对应的 connect 记录拿到 time
            LocalDateTime t = accepted.stream()
                    .filter(r -> r.getMemberId().equals(memberId))
                    .findFirst()
                    .map(TrainerConnectRequest::getCreateTime)
                    .orElse(null);

            result.add(ConnectedMemberVO.builder()
                    .memberId(memberId)
                    .memberName(name)
                    .memberEmail(email)
                    .connectTime(t)
                    .build());
        }
        return result;
    }

}

