package com.gym.vo;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TrainerBasicInfoVO {
    private Long trainerId;
    private String name;
}
