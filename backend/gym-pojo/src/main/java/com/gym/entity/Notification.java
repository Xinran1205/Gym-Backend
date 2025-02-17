package com.gym.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("notifications")
public class Notification implements Serializable {

    @TableId(value = "notification_id", type = IdType.AUTO)
    private Long notificationId;

    @TableField("user_id")
    private Long userId;

    @TableField("message")
    private String message;

    /**
     * type 为 VARCHAR(50)，这里直接用 String
     */
    @TableField("type")
    private String type;

    @TableField("is_read")
    private Boolean isRead = false;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
