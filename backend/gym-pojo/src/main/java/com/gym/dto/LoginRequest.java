package com.gym.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class LoginRequest {
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email is invalid")
    private String email;


    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 20, message = "Password length must be between 6 and 20 characters")
    private String password;
}
