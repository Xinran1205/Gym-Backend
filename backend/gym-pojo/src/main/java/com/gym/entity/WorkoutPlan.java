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
@TableName("workout_plans")
public class WorkoutPlan implements Serializable {

    @TableId(value = "plan_id", type = IdType.AUTO)
    private Long planId;

    @TableField("trainer_id")
    private Long trainerId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;   // small text or full markdown

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
