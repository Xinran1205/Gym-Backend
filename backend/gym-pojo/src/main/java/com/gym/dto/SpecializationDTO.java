package com.gym.dto;

import lombok.*;


/**
 * 用于新增 Specialization 的请求体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class SpecializationDTO {
    private String description;
}
