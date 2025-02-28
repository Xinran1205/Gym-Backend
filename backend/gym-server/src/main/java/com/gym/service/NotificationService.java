package com.gym.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.entity.Notification;

public interface NotificationService extends IService<Notification> {
    /**
     * 发送通知，将通知记录持久化
     *
     * @param notification 通知实体
     */
    void sendNotification(Notification notification);


    /**
     * 分页查询指定用户的通知
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页通知数据
     */
    Page<Notification> getNotificationsByUser(Long userId, int page, int pageSize);

    /**
     * 标记指定通知为已读
     *
     * @param notificationId 通知ID
     * @param userId         当前用户ID
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * 删除指定通知，前提是该通知已被标记为已读
     *
     * @param notificationId 通知ID
     * @param userId         当前用户ID
     */
    void deleteNotification(Long notificationId, Long userId);
}

