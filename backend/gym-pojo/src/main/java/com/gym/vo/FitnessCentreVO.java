package com.gym.vo;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class FitnessCentreVO {
    // 如果以后需要更复杂的连锁品牌信息，可以在这里扩展字段。现在暂时使用健身房的名称作为 title。
    private String title;

    // 英文地址，包含邮编
    private String address;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private String contactInfo;
}
