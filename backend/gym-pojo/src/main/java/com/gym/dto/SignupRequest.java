package com.gym.dto;

import com.gym.entity.User;
import lombok.*;

import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
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
    private LocalDate dateOfBirth;

    @NotNull(message = "Role cannot be null")
    private User.Role role;

    // 新增：腾讯验证码相关字段（由前端验证码组件返回）
    @NotBlank(message = "Captcha ticket cannot be blank")
    private String captchaTicket;

    @NotBlank(message = "Captcha randstr cannot be blank")
    private String captchaRandstr;
}
