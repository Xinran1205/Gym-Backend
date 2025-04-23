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
@TableName("appointment_alternative_trainer")
public class AppointmentAlternativeTrainer implements Serializable {

    @TableId(value = "appointment_id", type = IdType.INPUT)
    private Long appointmentId;

    @TableField("alternative_trainer_id")
    private Long alternativeTrainerId;

    @TableField("alternative_trainer_name")
    private String alternativeTrainerName;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
