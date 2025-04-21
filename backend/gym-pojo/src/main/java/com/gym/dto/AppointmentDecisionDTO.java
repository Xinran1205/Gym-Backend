package com.gym.dto;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AppointmentDecisionDTO {
    private Long appointmentId;

    private String responseMessage;
}
