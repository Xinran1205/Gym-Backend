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
    private String memberEmail;         // new: member's email
    private LocalDateTime connectTime;  // new: time when the member applied to connect
}
