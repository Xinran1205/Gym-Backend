package com.gym.dto;

import lombok.Data;

import java.util.Date;

@Data
public class SignupRequest {
    private String email;
    private String password;
    private String name;
    private String address;
    private Date dateOfBirth;
}
