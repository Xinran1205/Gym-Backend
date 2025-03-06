package com.gym.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AppointmentDecisionRejectDTO {
    private Long appointmentId;

    // 可选反馈信息
    // 拒绝的理由，会通过通知发送给用户
    private String responseMessage;

    // 教练的userid（指定替代的教练）
    private Long alternativeTrainerId;

    // 教练的姓名
    private String alternativeTrainerName;

}
