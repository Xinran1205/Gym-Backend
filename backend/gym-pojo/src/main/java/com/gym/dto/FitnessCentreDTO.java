package com.gym.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class FitnessCentreDTO {

    private String name;

    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String contactInfo;

    private String description;
}
