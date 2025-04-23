package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.dao.NotificationDao;
import com.gym.entity.Notification;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationServiceImpl extends ServiceImpl<NotificationDao, Notification> implements NotificationService {

    @Override
    // 直接通知表中插入通知
    public void sendNotification(Notification notification) {
        boolean saved = this.save(notification);
        if (!saved) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Failed to send notification.");
        }
        log.info("Notification sent to user [{}] with title [{}]", notification.getUserId(), notification.getTitle());
    }

    @Override
    public Page<Notification> getNotificationsByUser(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime);
        return this.page(new Page<>(page, pageSize), queryWrapper);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = this.getById(notificationId);
        if (notification == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Notification not found.");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not authorized to modify this notification.");
        }
        notification.setIsRead(true);
        boolean updateResult = this.updateById(notification);
        if (!updateResult) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Failed to mark notification as read.");
        }
    }

    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = this.getById(notificationId);
        if (notification == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Notification not found.");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not authorized to delete this notification.");
        }
        if (notification.getIsRead() == null || !notification.getIsRead()) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Notification must be marked as read before deletion.");
        }
        boolean removed = this.removeById(notificationId);
        if (!removed) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Failed to delete notification.");
        }
        log.info("Notification [{}] deleted for user [{}]", notificationId, userId);
    }
}

