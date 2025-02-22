package com.gym.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TrainerProfileDTO {

    @NotBlank(message = "Certifications cannot be blank")
    @Size(max = 200, message = "Certifications cannot be longer than 200 characters")
    private String certifications;

    @NotBlank(message = "Specializations cannot be blank")
    @Size(max = 200, message = "Specializations cannot be longer than 200 characters")
    private String specializations;

    @NotNull(message = "Years of experience cannot be null")
    @Min(value = 0, message = "Years of experience must be non-negative")
    private Integer yearsOfExperience;

    @NotBlank(message = "Biography cannot be blank")
    @Size(max = 500, message = "Biography cannot be longer than 500 characters")
    private String biography;
}
