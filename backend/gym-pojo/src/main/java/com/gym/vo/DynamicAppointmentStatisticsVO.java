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
     * 日期范围内的每日统计数据列表
     */
    private List<DailyStatisticVO> dailyStatistics;
}
