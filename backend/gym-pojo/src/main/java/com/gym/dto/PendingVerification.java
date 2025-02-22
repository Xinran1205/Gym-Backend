package com.gym.dto;

import lombok.*;


// 这个不需要校验，因为是后台生成的，不是用户输入的
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PendingVerification {
    private SignupRequest request;
    private String verificationCode;
    private long createTime;
}
