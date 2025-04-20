package com.gym.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TrainerConnectDecisionDTO {
//    @NotBlank(message = "Request ID cannot be blank")
    // 这个是那条申请记录的 ID
    private Long requestId;

    // 可选：教练处理该申请时的反馈信息
    private String responseMessage;
}
