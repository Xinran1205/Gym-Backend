package com.gym.dto;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class WorkoutPlanDTO {
    private String title;
    private String content;
}
