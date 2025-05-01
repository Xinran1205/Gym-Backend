package com.gym.dto;

import lombok.*;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForceBookingDTO {

    @NotNull(message = "availabilityId 不能为空")
    private Long availabilityId;   // 教练自己可用时段 ID

    @NotNull(message = "memberId 不能为空")
    private Long memberId;         // 要强制排课的学员 ID

    // ↓ 可选：为方便统计／展示，同时支持自定义项目名与备注
    private String projectName;
    private String description;
}
