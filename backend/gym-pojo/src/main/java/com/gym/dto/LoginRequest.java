package com.gym.dto;

import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class LoginRequest {
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email is invalid")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 20, message = "Password length must be between 6 and 20 characters")
    private String password;

    // 新增：腾讯验证码相关字段（由前端验证码组件返回）
    @NotBlank(message = "Captcha ticket cannot be blank")
    private String captchaTicket;

    @NotBlank(message = "Captcha randstr cannot be blank")
    private String captchaRandstr;
}
