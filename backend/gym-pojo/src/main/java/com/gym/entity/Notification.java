package com.gym.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@TableName("notifications")
public class Notification implements Serializable {

    @TableId(value = "notification_id", type = IdType.AUTO)
    private Long notificationId;

    @TableField("user_id")
    private Long userId; // 接收通知的用户ID

    @TableField("title")
    private String title; // 通知标题，例如 "申请结果通知"

    @TableField("message")
    private String message; // 通知内容，例如 "您的教练申请已被接受"

    @TableField("type")
    private NotificationType type; // 通知类型

    @TableField("is_read")
    private Boolean isRead; // 是否已读

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public enum NotificationType {
        INFO, ALERT, SYSTEM
    }
}

