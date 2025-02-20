package com.gym.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class UserEmail{
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email is invalid")
    private String email;
}
