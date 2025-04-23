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
@TableName("appointment_bookings")
public class AppointmentBooking implements Serializable {

    @TableId(value = "appointment_id", type = IdType.AUTO)
    private Long appointmentId;

    @TableField("member_id")
    private Long memberId;

    @TableField("trainer_id")
    private Long trainerId;

    @TableField("availability_id")
    private Long availabilityId;

    @TableField("project_name")
    private String projectName;

    @TableField("description")
    private String description;

    @TableField("workout_plan_id")   // NEW  FK (nullable)
    private Long workoutPlanId;

    @TableField("appointment_status")
    private AppointmentStatus appointmentStatus; // Pending, Approved, Rejected, Cancelled, Completed

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public enum AppointmentStatus {
        Pending, Approved, Rejected, Cancelled, Completed, Expired
    }
}

