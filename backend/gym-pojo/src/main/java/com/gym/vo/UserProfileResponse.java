package com.gym.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gym.entity.User;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class UserProfileResponse {

    private String name;

    private LocalDate dateOfBirth;

    private String email;

    private String address;

    private User.Role role;

    @JsonProperty("isGoogle")
    private boolean isGoogle;
}
