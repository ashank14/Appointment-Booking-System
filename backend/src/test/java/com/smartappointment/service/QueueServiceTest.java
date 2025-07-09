package com.smartappointment.service;

import com.smartappointment.entity.Appointment;
import com.smartappointment.entity.Slot;
import com.smartappointment.entity.User;
import com.smartappointment.repository.AppointmentRepository;
import com.smartappointment.repository.SlotRepository;
import com.smartappointment.util.enumerations.AppointmentStatus;
import com.smartappointment.util.enumerations.SlotStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueueServiceTest {

    @InjectMocks
    private QueueService queueService;

    @Mock
    private RedisTemplate<String, User> redisTemplate;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private ListOperations<String, User> listOperations;

    private User user;
    private Slot slot;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        user = User.builder().id(1L).username("user1").build();

        // Set times safely far in the future to avoid timezone problems
        LocalDateTime futureStartTime = LocalDateTime.now().plusDays(1);
        LocalDateTime futureEndTime = futureStartTime.plusHours(1);

        slot = new Slot();
        slot.setId(100L);
        slot.setStartTime(futureStartTime);
        slot.setEndTime(futureEndTime);
        slot.setStatus(SlotStatus.BOOKED);

        when(redisTemplate.opsForList()).thenReturn(listOperations);
    }
    @Test
    void testJoinQueueSuccess() {
        when(slotRepository.findById(100L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.findBySlotId(100L)).thenReturn(null);
        when(appointmentRepository.findByUser(user)).thenReturn(Collections.emptyList());
        when(redisTemplate.keys("queue:slot:*")).thenReturn(Collections.emptySet());

        queueService.joinQueue(100L, user);

        verify(listOperations).rightPush("queue:slot:100", user);
    }

    @Test
    void testJoinQueue_ClashingAppointment_ThrowsException() {
        when(slotRepository.findById(100L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.findBySlotId(100L)).thenReturn(null);

        Slot otherSlot = new Slot();
        // Set overlapping time with the test slot (which is already +1 day)
        otherSlot.setStartTime(slot.getStartTime().minusMinutes(30));
        otherSlot.setEndTime(slot.getStartTime().plusMinutes(30));

        Appointment existing = new Appointment();
        existing.setSlot(otherSlot);

        when(appointmentRepository.findByUser(user)).thenReturn(List.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                queueService.joinQueue(100L, user));

        assertTrue(ex.getMessage().contains("booked appointment"));
    }


    @Test
    void testDequeueNextAndBook() {
        when(listOperations.leftPop("queue:slot:100")).thenReturn(user);
        when(slotRepository.findById(100L)).thenReturn(Optional.of(slot));

        queueService.dequeueNextAndBook(100L);

        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void testLeaveQueue_UserPresent_RemovesUser() {
        List<User> queue = new ArrayList<>(List.of(user));

        when(redisTemplate.hasKey("queue:slot:100")).thenReturn(true);
        when(listOperations.range("queue:slot:100", 0, -1)).thenReturn(queue);

        queueService.leaveQueue(100L, user);

        verify(redisTemplate).delete("queue:slot:100");
        verify(listOperations, never()).rightPush(eq("queue:slot:100"), any());
    }

    @Test
    void testLeaveQueue_UserNotInQueue_Throws() {
        List<User> queue = new ArrayList<>();

        when(redisTemplate.hasKey("queue:slot:100")).thenReturn(true);
        when(listOperations.range("queue:slot:100", 0, -1)).thenReturn(queue);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                queueService.leaveQueue(100L, user));

        assertTrue(ex.getMessage().contains("not in the queue"));
    }

    @Test
    void testGetQueue() {
        List<User> users = List.of(user);
        when(listOperations.range("queue:slot:100", 0, -1)).thenReturn(users);

        List<User> result = queueService.getQueue(100L);

        assertEquals(1, result.size());
        assertEquals("user1", result.get(0).getUsername());
    }

    @Test
    void testRemoveQueue() {
        queueService.removeQueue(100L);
        verify(redisTemplate).delete("queue:slot:100");
    }

}
