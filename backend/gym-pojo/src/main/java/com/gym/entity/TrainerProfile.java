package com.gym.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("trainer_profiles")
public class TrainerProfile implements Serializable {

    @TableId(value = "trainer_profile_id", type = IdType.AUTO)
    private Long trainerProfileId;

    @TableField("user_id")
    private Long userId;

    @TableField("certifications")
    private String certifications;

    @TableField("specializations")
    private String specializations;

    @TableField("years_of_experience")
    private Integer yearsOfExperience;

    @TableField("biography")
    private String biography;

    /**
     * availability 字段在表结构中是 JSON 类型（如果数据库不支持JSON类型，可能为TEXT）。
     * 这里直接用 String 来接收（如需更复杂的JSON处理，可考虑再做转换）。
     */
    @TableField("availability")
    private String availability;

    /**
     * rating 字段是 DECIMAL(3,2)，可用 BigDecimal、Double 或 Float。
     * 这里采用 BigDecimal 更精确。
     */
    @TableField("rating")
    private BigDecimal rating;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
