package com.gym.vo;

import com.gym.entity.TrainerProfile;
import com.gym.entity.User;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TrainerAllProfile {
    private TrainerProfile trainerProfile;
    private UserProfileResponse userProfileResponse;
}
