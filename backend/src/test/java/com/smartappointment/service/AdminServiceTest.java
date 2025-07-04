package com.smartappointment.service;

import com.smartappointment.dto.AppointmentResponseDto;
import com.smartappointment.entity.Appointment;
import com.smartappointment.entity.CancellationLog;
import com.smartappointment.entity.Slot;
import com.smartappointment.entity.User;
import com.smartappointment.repository.AppointmentRepository;
import com.smartappointment.repository.CancellationLogRepository;
import com.smartappointment.repository.UserRepository;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    private AdminService adminService;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CancellationLogRepository cancellationLogRepository;

    private User provider;
    private Slot slot;
    private Appointment appointment;
    private CancellationLog logEntry;

    @BeforeEach
    void setup() {
        provider = User.builder().id(1L).username("provider1").role("PROVIDER").build();

        slot = new Slot();
        slot.setId(100L);
        slot.setDescription("Consult");
        slot.setStartTime(LocalDateTime.now().plusHours(1));
        slot.setEndTime(LocalDateTime.now().plusHours(2));
        slot.setProvider(provider);

        appointment = Appointment.builder().id(200L).slot(slot).build();

        logEntry = new CancellationLog();
        logEntry.setId(300L);
        logEntry.setProviderId(1L);
    }

    @Test
    void testGetTotalAppointmentsForProviderSuccess() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(appointmentRepository.findBySlotProviderId(1L)).thenReturn(List.of(appointment));

        long total = adminService.getTotalAppointmentsForProvider(1L);

        assertEquals(1, total);
    }

    @Test
    void testGetTotalAppointmentsForProvider_NotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> adminService.getTotalAppointmentsForProvider(1L));
    }

    @Test
    void testGetProviderAppointments() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(appointmentRepository.findBySlotProviderId(1L)).thenReturn(List.of(appointment));

        List<AppointmentResponseDto> result = adminService.getProviderAppointments(1L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getSlotId());
    }

    @Test
    void testGetCancellationsForProvider() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(cancellationLogRepository.findByProviderId(1L)).thenReturn(List.of(logEntry));

        long result = adminService.getCancellationsForProvider(1L);
        assertEquals(1, result);
    }

    @Test
    void testGetPeakBookingHours() {
        appointment.getSlot().setStartTime(LocalDateTime.of(2024, 1, 1, 10, 0));
        when(appointmentRepository.findAll()).thenReturn(List.of(appointment));

        Map<Integer, Long> result = adminService.getPeakBookingHours();

        assertEquals(1, result.get(10));
    }

    @Test
    void testGetCancellationRateForProvider() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(cancellationLogRepository.findByProviderId(1L)).thenReturn(List.of(logEntry));
        when(appointmentRepository.findBySlotProviderId(1L)).thenReturn(List.of(appointment));

        double rate = adminService.getCancellationRateForProvider(1L);

        assertEquals(50.0, rate);
    }

    @Test
    void testGetCancellationRateZeroAppointments() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(provider));
        when(cancellationLogRepository.findByProviderId(1L)).thenReturn(Collections.emptyList());
        when(appointmentRepository.findBySlotProviderId(1L)).thenReturn(Collections.emptyList());

        double rate = adminService.getCancellationRateForProvider(1L);

        assertEquals(0.0, rate);
    }
}
