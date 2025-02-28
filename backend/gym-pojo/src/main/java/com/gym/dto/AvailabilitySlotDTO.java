package com.gym.dto;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AvailabilitySlotDTO {
    // @NotNull(message = "Start time cannot be null")
    private LocalDateTime startTime;

    // @NotNull(message = "End time cannot be null")
    private LocalDateTime endTime;
}
