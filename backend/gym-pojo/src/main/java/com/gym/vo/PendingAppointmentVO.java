package com.gym.vo;


import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PendingAppointmentVO {
    private Long   appointmentId;
    private Long   memberId;
    private String memberName;     // 新增
    private String projectName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime startTime;  // 课程开始
    private LocalDateTime endTime;    // 课程结束

    // 新增：绑定的 WorkoutPlan 信息（如果有）
    // 新增：绑定的 WorkoutPlan 信息
    private Long   workoutPlanId;
    private String workoutPlanTitle;
    private String workoutPlanContent;
}
