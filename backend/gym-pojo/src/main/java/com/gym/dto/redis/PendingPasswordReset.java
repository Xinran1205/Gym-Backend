package com.gym.dto.redis;

import lombok.Data;

import java.io.Serializable;

@Data
public class PendingPasswordReset implements Serializable {
    private String email;
    private String resetCode;   // 验证码
    private long createTime;    // 生成时间，用于必要时做额外校验
}
