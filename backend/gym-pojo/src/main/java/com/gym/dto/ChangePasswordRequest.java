package com.gym.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ChangePasswordRequest {
    @NotBlank(message = "oldPassword cannot be blank")
    private String oldPassword;

    @NotBlank(message = "newPassword cannot be blank")
    private String newPassword;
}
