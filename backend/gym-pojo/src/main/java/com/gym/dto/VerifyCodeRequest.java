package com.gym.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class VerifyCodeRequest {
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email is invalid")
    private String email;

    @NotBlank(message = "Verification code cannot be blank")
    @Size(min = 4, max = 10, message = "Verification code length must be between 4 and 10 characters")
    private String code;
}
