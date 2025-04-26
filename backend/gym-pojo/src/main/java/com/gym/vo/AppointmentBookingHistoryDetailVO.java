package com.gym.vo;

import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AppointmentBookingHistoryDetailVO {
    private Long appointmentId;
    private String projectName;
    private String description;
    private String appointmentStatus; // Pending, Approved, Rejected, Cancelled, Completed, Expired
    private LocalDateTime bookingCreatedAt; // 预约申请提交的时间
    private LocalDateTime sessionStartTime; // 对应可用时间开始时间
    private LocalDateTime sessionEndTime;   // 对应可用时间结束时间
    private String trainerName;             // 从用户表中查出的教练名称

    // 替代教练的userid
    private Long alternativeTrainerId;
    // 替代教练的姓名
    private String alternativeTrainerName;

    // ★ 新增
    private String         workoutPlanTitle;
    private String         workoutPlanContent;
}
