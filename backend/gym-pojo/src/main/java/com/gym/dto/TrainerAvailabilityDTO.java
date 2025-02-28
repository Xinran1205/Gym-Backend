package com.gym.dto;


import lombok.*;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TrainerAvailabilityDTO {
    // @NotEmpty(message = "Availability slots cannot be empty")
    private List<AvailabilitySlotDTO> availabilitySlots;
}
