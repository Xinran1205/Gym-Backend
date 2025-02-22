package com.gym.vo;

import com.gym.entity.User;
import lombok.*;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class UserProfileResponse {

    private String name;

    private Date dateOfBirth;

    private String address;
}
