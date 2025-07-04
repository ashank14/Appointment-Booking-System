package com.smartappointment.service;

import com.smartappointment.config.AppConfigProperties;
import com.smartappointment.dto.AppointmentResponseDto;
import com.smartappointment.entity.*;
import com.smartappointment.notification.NotificationService;
import com.smartappointment.notification.kafka.producer.KafkaProducerService;
import com.smartappointment.repository.*;
import com.smartappointment.util.enumerations.AppointmentStatus;
import com.smartappointment.util.enumerations.SlotStatus;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @InjectMocks
    private AppointmentService appointmentService;

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SlotRepository slotRepository;
    @Mock
    private CancellationLogRepository cancellationLogRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private KafkaProducerService kafkaProducerService;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private AppConfigProperties config;
    @Mock
    private QueueService queueService;
    @Mock
    private io.micrometer.core.instrument.Counter counter;


    private User user;
    private User provider;
    private Slot slot;
    private Appointment appointment;

    @BeforeEach
    void setup() {
        user = User.builder().id(1L).username("user1").email("user1@mail.com").role("CUSTOMER").build();
        provider = User.builder().id(2L).username("provider1").email("p@mail.com").role("PROVIDER").build();

        slot = new Slot();
        slot.setId(100L);
        slot.setStatus(SlotStatus.AVAILABLE);
        slot.setStartTime(LocalDateTime.now().plusHours(1));
        slot.setEndTime(LocalDateTime.now().plusHours(2));
        slot.setProvider(provider);
        slot.setDescription("Consult");

        appointment = Appointment.builder()
                .id(200L)
                .slot(slot)
                .user(user)
                .status(AppointmentStatus.BOOKED)
                .build();
    }

    @Test
    void testCreateAppointmentSuccess() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(slotRepository.findById(100L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.findByUser(user)).thenReturn(Collections.emptyList());
        when(config.getMaxBookingsPerDay()).thenReturn(5);
        when(appointmentRepository.save(any())).thenReturn(appointment);
        when(meterRegistry.counter(eq("appointments.booked.count"))).thenReturn(counter);

        AppointmentResponseDto response = appointmentService.createAppointment(100L, "user1");

        assertNotNull(response);
        assertEquals(100L, response.getSlotId());
        assertEquals("AVAILABLE", response.getStatus());
        verify(notificationService).sendNotification(anyString(), contains("booked successfully"));
        verify(meterRegistry).counter("appointments.booked.count");
    }

    @Test
    void testCreateAppointment_SlotNotAvailable() {
        slot.setStatus(SlotStatus.BOOKED);
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(slotRepository.findById(100L)).thenReturn(Optional.of(slot));

        assertThrows(IllegalStateException.class, () -> appointmentService.createAppointment(100L, "user1"));
    }

    @Test
    void testDeleteAppointmentSuccess() {
        when(appointmentRepository.findById(200L)).thenReturn(Optional.of(appointment));

        // 👇 Fix: mock the counter for cancellations
        when(meterRegistry.counter(eq("appointments.cancelled.count"))).thenReturn(counter);

        appointmentService.deleteAppointment(200L, "user1");

        verify(slotRepository).save(slot);
        verify(cancellationLogRepository).save(any(CancellationLog.class));
        verify(appointmentRepository).delete(appointment);
        verify(queueService).dequeueNextAndBook(100L);
        verify(notificationService).sendNotification(eq("user1@mail.com"), contains("cancelled successfully"));

        // Optional verify counter increment
        verify(counter).increment();
    }
    @Test
    void testDeleteAppointment_Unauthorized() {
        when(appointmentRepository.findById(200L)).thenReturn(Optional.of(appointment));

        assertThrows(ResponseStatusException.class, () -> appointmentService.deleteAppointment(200L, "someoneelse"));
    }

    @Test
    void testCompleteAppointmentSuccess() {
        when(appointmentRepository.findById(200L)).thenReturn(Optional.of(appointment));

        // appointment time within slot's start & end time
        slot.setStartTime(LocalDateTime.now().minusMinutes(30));
        slot.setEndTime(LocalDateTime.now().plusMinutes(30));

        appointmentService.completeAppointment(200L, "provider1");

        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
        assertEquals(SlotStatus.EXPIRED, appointment.getSlot().getStatus());
        verify(slotRepository).save(slot);
        verify(appointmentRepository).save(appointment);
        verify(notificationService).sendNotification(eq("user1@mail.com"), contains("completed successfully"));
    }

    @Test
    void testCompleteAppointmentOutsideSlotTime() {
        when(appointmentRepository.findById(200L)).thenReturn(Optional.of(appointment));

        // slot time in future
        slot.setStartTime(LocalDateTime.now().plusHours(1));

        assertThrows(ResponseStatusException.class, () -> appointmentService.completeAppointment(200L, "provider1"));
    }

    @Test
    void testGetUserAppointments() {
        when(appointmentRepository.findByUserUsername("user1")).thenReturn(List.of(appointment));

        List<AppointmentResponseDto> result = appointmentService.getUserAppointments("user1");

        assertEquals(1, result.size());
        assertEquals("Consult", result.get(0).getDescription());
    }

    @Test
    void testGetProviderAppointments() {
        when(appointmentRepository.findBySlotProviderUsername("provider1")).thenReturn(List.of(appointment));

        List<AppointmentResponseDto> result = appointmentService.getProviderAppointments("provider1");

        assertEquals(1, result.size());
        assertEquals("Consult", result.get(0).getDescription());
    }

    @Test
    void testGetByIdSuccess() {
        when(appointmentRepository.findById(200L)).thenReturn(Optional.of(appointment));

        AppointmentResponseDto response = appointmentService.getById(200L, "user1");

        assertEquals(200L, response.getAppointmentId());
        assertEquals("Consult", response.getDescription());
    }

    @Test
    void testGetByIdUnauthorized() {
        when(appointmentRepository.findById(200L)).thenReturn(Optional.of(appointment));

        assertThrows(EntityNotFoundException.class, () -> appointmentService.getById(200L, "otheruser"));
    }
}
