package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.dao.AppointmentAlternativeTrainerDao;
import com.gym.dao.AppointmentBookingDao;
import com.gym.dto.AppointmentBookingDTO;
import com.gym.dto.AppointmentDecisionDTO;
import com.gym.dto.AppointmentDecisionRejectDTO;
import com.gym.entity.*;
import com.gym.exception.CustomException;
import com.gym.service.NotificationService;
import com.gym.service.TrainerAvailabilityService;
import com.gym.service.TrainerConnectRequestService;
import com.gym.vo.AppointmentBookingDetailVO;
import com.gym.vo.AppointmentBookingHistoryDetailVO;
import com.gym.vo.DailyStatisticVO;
import com.gym.vo.DynamicAppointmentStatisticsVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentBookingServiceImplTest {

    @Mock
    private TrainerConnectRequestService mockTrainerConnectRequestService;
    @Mock
    private TrainerAvailabilityService mockTrainerAvailabilityService;

    @Mock
    private AppointmentBookingDao mockAppointmentBookingDao;

    @InjectMocks
    private AppointmentBookingServiceImpl appointmentBookingServiceImplUnderTest;

    @Test
    void testBookSession_TrainerConnectRequestServiceReturnsNull() {
        // Setup
        final AppointmentBookingDTO dto = AppointmentBookingDTO.builder()
                .trainerId(0L)
                .availabilityId(0L)
                .projectName("projectName")
                .description("description")
                .build();
        when(mockTrainerConnectRequestService.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // Run the test
        assertThatThrownBy(() -> appointmentBookingServiceImplUnderTest.bookSession(dto, 0L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void testBookSession_TrainerAvailabilityServiceGetByIdReturnsNull() {
        // Setup
        final AppointmentBookingDTO dto = AppointmentBookingDTO.builder()
                .trainerId(0L)
                .availabilityId(0L)
                .projectName("projectName")
                .description("description")
                .build();

        // Configure TrainerConnectRequestService.getOne(...).
        final TrainerConnectRequest trainerConnectRequest = TrainerConnectRequest.builder()
                .memberId(0L)
                .trainerId(0L)
                .status(TrainerConnectRequest.RequestStatus.Pending)
                .build();
        when(mockTrainerConnectRequestService.getOne(any(LambdaQueryWrapper.class))).thenReturn(trainerConnectRequest);

        when(mockTrainerAvailabilityService.getById(0L)).thenReturn(null);

        // Run the test
        assertThatThrownBy(() -> appointmentBookingServiceImplUnderTest.bookSession(dto, 0L))
                .isInstanceOf(CustomException.class);
    }
    @Test
    void testGetDynamicAppointmentStatisticsForMember() {
        // Setup
        final DynamicAppointmentStatisticsVO expectedResult = DynamicAppointmentStatisticsVO.builder()
                .dailyStatistics(Arrays.asList(DailyStatisticVO.builder()
                        .date(LocalDate.of(2020, 1, 1))
                        .hours(0.0)
                        .build()))
                .build();

        // Configure AppointmentBookingDao.selectDynamicStatisticsByMember(...).
        final List<DailyStatisticVO> dailyStatisticVOS = Arrays.asList(DailyStatisticVO.builder()
                .date(LocalDate.of(2020, 1, 1))
                .hours(0.0)
                .build());
        when(mockAppointmentBookingDao.selectDynamicStatisticsByMember(0L, LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 1, 1))).thenReturn(dailyStatisticVOS);

        // Run the test
        final DynamicAppointmentStatisticsVO result = appointmentBookingServiceImplUnderTest.getDynamicAppointmentStatisticsForMember(
                0L, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1));

        // Verify the results
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void testGetDynamicAppointmentStatisticsForMember_AppointmentBookingDaoReturnsNoItems() {
        // Setup
        final DynamicAppointmentStatisticsVO expectedResult = DynamicAppointmentStatisticsVO.builder()
                .dailyStatistics(Arrays.asList(DailyStatisticVO.builder()
                        .date(LocalDate.of(2020, 1, 1))
                        .hours(0.0)
                        .build()))
                .build();
        when(mockAppointmentBookingDao.selectDynamicStatisticsByMember(0L, LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 1, 1))).thenReturn(Collections.emptyList());

        // Run the test
        final DynamicAppointmentStatisticsVO result = appointmentBookingServiceImplUnderTest.getDynamicAppointmentStatisticsForMember(
                0L, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1));

        // Verify the results
        assertThat(result).isEqualTo(expectedResult);
    }
}
