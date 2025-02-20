package com.gym.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.util.Date;

@Data
public class SignupRequest {
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email is invalid")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 20, message = "Password length must be between 6 and 20 characters")
    private String password;

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 50, message = "Name cannot be longer than 50 characters")
    private String name;

    @NotBlank(message = "Address cannot be blank")
    private String address;

    @NotNull(message = "Date of birth cannot be null")
    @Past(message = "Date of birth must be in the past")
    private Date dateOfBirth;
}
