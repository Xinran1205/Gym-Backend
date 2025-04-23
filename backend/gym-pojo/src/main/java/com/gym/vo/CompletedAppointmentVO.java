package com.gym.vo;

import lombok.*;
import java.time.LocalDateTime;

/**
 * 已完成预约项，包含学员姓名
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class CompletedAppointmentVO {
    private Long appointmentId;
    private Long memberId;
    private String memberName;
    private String projectName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime startTime;  // 新增
    private LocalDateTime endTime;    // 新增
}
