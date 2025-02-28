package com.gym.dto;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AppointmentDecisionDTO {
    private Long appointmentId;

    // 可选反馈信息
    private String responseMessage;
}
