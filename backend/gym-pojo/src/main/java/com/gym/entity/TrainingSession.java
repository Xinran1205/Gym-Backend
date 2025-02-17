package com.gym.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("training_sessions")
public class TrainingSession implements Serializable {

    @TableId(value = "session_id", type = IdType.AUTO)
    private Long sessionId;

    @TableField("request_id")
    private Long requestId;

    @TableField("member_id")
    private Long memberId;

    @TableField("trainer_id")
    private Long trainerId;

    /**
     * scheduled_at 是 TIMESTAMP，不妨直接用 LocalDateTime
     */
    @TableField("scheduled_at")
    private LocalDateTime scheduledAt;

    /**
     * duration 表示时长(分钟/小时)，根据业务需求定义，这里使用 Integer
     */
    @TableField("duration")
    private Integer duration;

    @TableField("status")
    private SessionStatus status = SessionStatus.Scheduled;

    @TableField("session_notes")
    private String sessionNotes;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 对应 ENUM('Scheduled','Completed','Cancelled','No-Show') NOT NULL DEFAULT 'Scheduled'
     * 枚举中不能使用中划线，所以可自行改成 NoShow 或其它写法
     */
    public enum SessionStatus {
        Scheduled, Completed, Cancelled, NoShow
    }
}
