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
     * 统计日期
     */
    private LocalDate date;
    /**
     * 当天完成的课程小时数（每条 Completed 记录代表1小时）
     */
    private Integer hours;
}
