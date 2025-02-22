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
@TableName("training_history")
public class TrainingHistory implements Serializable {

    @TableId(value = "history_id", type = IdType.AUTO)
    private Long historyId;

    @TableField("member_id")
    private Long memberId;

    @TableField("session_id")
    private Long sessionId;

    @TableField(value = "recorded_at", fill = FieldFill.INSERT)
    private LocalDateTime recordedAt;

    @TableField("duration")
    private Integer duration;

    @TableField("session_notes")
    private String sessionNotes;
}
