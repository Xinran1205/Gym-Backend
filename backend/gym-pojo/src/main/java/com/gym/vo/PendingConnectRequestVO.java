package com.gym.vo;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Slim view of a connect request for trainers.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PendingConnectRequestVO {
    private Long requestId;
    private Long memberId;
    private String memberName;
    private String requestMessage;
    private LocalDateTime createdAt;
}
