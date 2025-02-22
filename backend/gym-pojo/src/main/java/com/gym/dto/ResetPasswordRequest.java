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
public class ResetPasswordRequest {

    @NotBlank(message = "token cannot be blank")
    private String token;  // JWT token

    @NotBlank(message = "newPassword cannot be blank")
    @Size(min = 6, max = 20, message = "Password length must be between 6 and 20 characters")
    private String newPassword; // 新密码
}
