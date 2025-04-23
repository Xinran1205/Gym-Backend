package com.gym.vo;

import lombok.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AppointmentWithTimeVO {
    private Long appointmentId;
    private String projectName;
    private String description;
    private String appointmentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime startTime;   // 预约开始时间
    private LocalDateTime endTime;     // 预约结束时间
}
