package com.gym.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AppointmentCompleteDTO {

    @NotNull(message = "appointmentId must not be null")
    private Long appointmentId;
}
