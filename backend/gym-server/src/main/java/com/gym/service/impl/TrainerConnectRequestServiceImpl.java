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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TrainerConnectRequestServiceImpl extends ServiceImpl<TrainerConnectRequestDao, TrainerConnectRequest>
        implements TrainerConnectRequestService {

    @Autowired
    private NotificationService notificationService;

    // 统计指定 member 当前待审核（Pending）状态的连接申请数量
    @Override
    public int countPendingRequests(Long memberId) {
        LambdaQueryWrapper<TrainerConnectRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrainerConnectRequest::getMemberId, memberId)
                .eq(TrainerConnectRequest::getStatus, TrainerConnectRequest.RequestStatus.Pending);
        return this.count(queryWrapper);
    }

    @Override
    public void submitConnectRequest(TrainerConnectRequestDTO dto, Long memberId) {
        TrainerConnectRequest request = TrainerConnectRequest.builder()
                .memberId(memberId)
                .trainerId(dto.getTrainerId())
                .status(TrainerConnectRequest.RequestStatus.Pending)
                .requestMessage(dto.getRequestMessage())
                .build();
        this.save(request);
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
}

