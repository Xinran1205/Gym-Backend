package com.gym.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@TableName("trainer_profiles")
public class TrainerProfile implements Serializable {

    @TableId(value = "trainer_profile_id", type = IdType.AUTO)
    private Long trainerProfileId;

    @TableField("user_id")
    private Long userId;

    // 增加教练名字，这样就不用再去查 user 表了
    @TableField("name")
    private String name;

    @TableField("certifications")
    private String certifications;

    @TableField("specializations")
    private String specializations;

    @TableField("years_of_experience")
    private Integer yearsOfExperience;

    @TableField("biography")
    private String biography;

    // 需要再来个信息，上班的地点！
    @TableField("workplace")
    private String workplace;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
