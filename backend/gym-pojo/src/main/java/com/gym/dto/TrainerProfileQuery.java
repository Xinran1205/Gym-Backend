package com.gym.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TrainerProfileQuery {
    private Integer page = 1;     // 默认第 1 页
    private Integer pageSize = 10; // 默认 10 条

    private String specializations;
    private String workplace;
}
