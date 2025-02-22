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
@TableName("training_requests")
public class TrainingRequest implements Serializable {

    @TableId(value = "request_id", type = IdType.AUTO)
    private Long requestId;

    @TableField("member_id")
    private Long memberId;

    /**
     * trainer_id 可能为 NULL，因此使用包装类型 Long
     */
    @TableField("trainer_id")
    private Long trainerId;

    @TableField("fitness_goal_description")
    private String fitnessGoalDescription;

    @TableField("status")
    private RequestStatus status = RequestStatus.Pending;

    @TableField("alternative_trainer_id")
    private Long alternativeTrainerId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 对应 ENUM('Pending', 'Accepted', 'Rejected', 'AlternativeSuggested') NOT NULL DEFAULT 'Pending'
     */
    public enum RequestStatus {
        Pending, Accepted, Rejected, AlternativeSuggested
    }
}
