package com.gym.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.*;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TrainerProfileVO {

    private String name;

    private String certifications;

    private String specializations;

    private Integer yearsOfExperience;

    private String biography;

    private String workplace;

}
