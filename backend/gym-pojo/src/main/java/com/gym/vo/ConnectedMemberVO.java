package com.gym.vo;

import lombok.*;

import java.time.LocalDateTime;

/** A lightweight view of a member who is already connected with the trainer */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ConnectedMemberVO {
    private Long   memberId;
    private String memberName;
    private String memberEmail;         // 新增：邮箱
    private LocalDateTime connectTime;  // 新增：申请 connect 时的时间
}
