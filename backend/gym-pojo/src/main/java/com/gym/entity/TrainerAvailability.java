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
@TableName("trainer_availability")
public class TrainerAvailability implements Serializable {

    @TableId(value = "availability_id", type = IdType.AUTO)
    private Long availabilityId;

    @TableField("trainer_id")
    private Long trainerId;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("status")
    private AvailabilityStatus status; // Available, Booked, Unavailable

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 这里感觉不需要unavailable
    public enum AvailabilityStatus {
        Available, Booked, Unavailable
    }
}

