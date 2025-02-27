package com.gym.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class UserProfileDTO {

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 50, message = "Name cannot be longer than 50 characters")
    private String name;

    @NotNull(message = "Date of birth cannot be null")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Address cannot be blank")
    private String address;
}
