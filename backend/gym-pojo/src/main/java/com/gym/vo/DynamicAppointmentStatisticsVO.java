package com.gym.vo;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class DynamicAppointmentStatisticsVO {
    /**
     * List of daily statistics data within the date range
     */
    private List<DailyStatisticVO> dailyStatistics;
}
