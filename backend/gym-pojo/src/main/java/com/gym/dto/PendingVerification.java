package com.gym.dto;

import lombok.Data;

@Data
public class PendingVerification {
    private SignupRequest request;
    private String verificationCode;
    private long createTime;
}
