package com.gym.vo;

import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class DailyStatisticVO {
    /**
     * The date for the statistic
     */
    private LocalDate date;
    /**
     * The number of completed sessions (1 hour each) on that day
     */
    private Integer hours;
}
