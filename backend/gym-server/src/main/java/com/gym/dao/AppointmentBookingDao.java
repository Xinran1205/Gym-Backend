package com.gym.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.entity.AppointmentBooking;
import com.gym.vo.AppointmentBookingDetailVO;
import com.gym.vo.DailyStatisticVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AppointmentBookingDao extends BaseMapper<AppointmentBooking> {

    @Select("<script>" +
            "SELECT ab.appointment_id, ab.project_name, ab.description, " +
            "       ab.appointment_status, ab.created_at AS bookingCreatedAt, " +
            "       ta.start_time AS sessionStartTime, ta.end_time AS sessionEndTime, " +
            "       u.name AS trainerName " +
            "FROM appointment_bookings ab " +
            "JOIN trainer_availability ta ON ab.availability_id = ta.availability_id " +
            "JOIN users u ON ab.trainer_id = u.user_id " +
            "WHERE ab.member_id = #{memberId} " +
            "AND ta.start_time &gt; NOW() " +
            " <choose> " +
            "   <when test='status != null and status != \"\"'> " +
            "       AND ab.appointment_status = #{status} " +
            "   </when> " +
            "   <otherwise> " +
            "       AND ab.appointment_status IN ('Pending','Approved') " +
            "   </otherwise> " +
            " </choose> " +
            "ORDER BY ta.start_time" +
            "</script>")
    Page<AppointmentBookingDetailVO> selectUpcomingAppointmentsByMember(Page<?> page,
                                                                        @Param("memberId") Long memberId,
                                                                        @Param("status") String status);
    @Select("<script>" +
            "SELECT ab.appointment_id, ab.project_name, ab.description, " +
            "       ab.appointment_status, ab.created_at AS bookingCreatedAt, " +
            "       ta.start_time AS sessionStartTime, ta.end_time AS sessionEndTime, " +
            "       u.name AS trainerName " +
            "FROM appointment_bookings ab " +
            "JOIN trainer_availability ta ON ab.availability_id = ta.availability_id " +
            "JOIN users u ON ab.trainer_id = u.user_id " +
            "WHERE ab.member_id = #{memberId} " +
            " <choose> " +
            "   <when test='status != null and status != \"\"'> " +
            "       AND ab.appointment_status = #{status} " +
            "   </when> " +
            "   <otherwise> " +
            "       AND ab.appointment_status NOT IN ('Pending','Approved') " +
            "   </otherwise> " +
            " </choose> " +
            "ORDER BY ta.start_time DESC" +
            "</script>")
    Page<AppointmentBookingDetailVO> selectHistoricalAppointmentsByMember(Page<?> page,
                                                                          @Param("memberId") Long memberId,
                                                                          @Param("status") String status);

    @Select("<script>" +
            "SELECT DATE(ta.end_time) AS date, COUNT(*) AS hours " +
            "FROM appointment_bookings ab " +
            "JOIN trainer_availability ta ON ab.availability_id = ta.availability_id " +
            "WHERE ab.member_id = #{memberId} " +
            "  AND ab.appointment_status = 'Completed' " +
            "  AND ta.end_time BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY DATE(ta.end_time) " +
            "ORDER BY date ASC" +
            "</script>")
    List<DailyStatisticVO> selectDynamicStatisticsByMember(@Param("memberId") Long memberId,
                                                           @Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate);
}


