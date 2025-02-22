package com.gym.dto;

import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class UserEmail{
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email is invalid")
    private String email;
}
