package com.gym.vo;

import com.gym.entity.AppointmentBooking;
import lombok.*;

import java.util.List;

/**
 * 教练端：一个学员对应的一组预约
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class MemberAppointmentsVO {
    private Long memberId;
    private String memberName;
    private List<AppointmentWithTimeVO> appointments;  // 使用新 VO
}
