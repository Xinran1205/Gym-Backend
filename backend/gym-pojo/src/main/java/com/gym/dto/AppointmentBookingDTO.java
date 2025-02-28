package com.gym.dto;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AppointmentBookingDTO {
    // @NotNull(message = "Trainer id is required")
    private Long trainerId;

    // @NotNull(message = "Availability id is required")
    private Long availabilityId;

    // @NotBlank(message = "Project name is required")
    private String projectName;

    // 可选描述
    private String description;
}

