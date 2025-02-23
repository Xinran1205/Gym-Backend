package com.gym.vo;

import com.gym.entity.TrainerProfile;
import com.gym.entity.User;
import lombok.*;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TrainerAllProfile {
    private TrainerProfile trainerProfile;

    private Date dateOfBirth;

    private String address;
}
