package com.gym.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("social_media_auth")
public class SocialMediaAuth implements Serializable {

    @TableId(value = "auth_id", type = IdType.AUTO)
    private Long authId;

    @TableField("user_id")
    private Long userId;

    @TableField("platform")
    private Platform platform;

    @TableField("social_media_user_id")
    private String socialMediaUserId;

    @TableField("access_token")
    private String accessToken;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 对应 ENUM('Google', 'Facebook')
     */
    public enum Platform {
        Google, Facebook
    }
}
