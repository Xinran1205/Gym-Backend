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
@TableName("trainer_connect_requests")
public class TrainerConnectRequest implements Serializable {

    @TableId(value = "request_id", type = IdType.AUTO)
    private Long requestId;

    @TableField("member_id")
    private Long memberId;

    @TableField("trainer_id")
    private Long trainerId;

    @TableField("status")
    private RequestStatus status; // pending, accepted, rejected

    @TableField("request_message")
    private String requestMessage;

    @TableField("response_message")
    private String responseMessage;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public enum RequestStatus {
        Pending, Accepted, Rejected, NONE
    }
}

