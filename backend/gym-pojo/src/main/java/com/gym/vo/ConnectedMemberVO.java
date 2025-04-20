package com.gym.vo;

import lombok.*;

/** A lightweight view of a member who is already connected with the trainer */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ConnectedMemberVO {
    private Long   memberId;
    private String memberName;
}
