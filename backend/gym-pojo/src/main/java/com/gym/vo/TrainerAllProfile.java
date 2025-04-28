package com.gym.vo;

import com.gym.entity.TrainerProfile;
import com.gym.entity.User;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TrainerAllProfile {
    private TrainerProfile trainerProfile;

    private LocalDate dateOfBirth;

    private String address;

    private String email;
}
