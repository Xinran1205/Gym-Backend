package com.gym.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@TableName("admin_profiles")
public class AdminProfile implements Serializable {

    @TableId(value = "admin_profile_id", type = IdType.AUTO)
    private Long adminProfileId;

    @TableField("user_id")
    private Long userId;

    @TableField("admin_level")
    private AdminLevel adminLevel;

    @TableField("additional_info")
    private String additionalInfo;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 枚举对应数据库的 ENUM('Super','Standard')
     */
    public enum AdminLevel {
        Super, Standard
    }
}
