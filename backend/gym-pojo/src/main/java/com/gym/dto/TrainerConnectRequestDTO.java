package com.gym.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TrainerConnectRequestDTO {
//    @NotBlank(message = "Trainer ID cannot be blank")
    private Long trainerId;

    private String requestMessage;
}

