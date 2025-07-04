package com.smartappointment.service;

import com.smartappointment.config.AppConfigProperties;
import com.smartappointment.dto.SlotRequestDto;
import com.smartappointment.dto.SlotResponseDto;
import com.smartappointment.entity.Slot;
import com.smartappointment.entity.User;
import com.smartappointment.notification.NotificationService;
import com.smartappointment.notification.kafka.producer.KafkaProducerService;
import com.smartappointment.repository.SlotRepository;
import com.smartappointment.repository.UserRepository;
import com.smartappointment.util.enumerations.SlotStatus;
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
class SlotServiceTest {

    @InjectMocks
    private SlotService slotService;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppConfigProperties config;

    @Mock
    private QueueService queueService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    private User provider;
    private Slot slot;

    @BeforeEach
    void setup() {
        provider = User.builder().id(1L).username("provider1").email("p@example.com").role("PROVIDER").specialization("Cardiology").build();

        slot = new Slot();
        slot.setId(10L);
        slot.setDescription("Consult");
        slot.setStartTime(LocalDateTime.now().plusHours(1));
        slot.setEndTime(LocalDateTime.now().plusHours(2));
        slot.setStatus(SlotStatus.AVAILABLE);
        slot.setProvider(provider);


    }

    @Test
    void testAddSlotSuccess() {
        SlotRequestDto request = new SlotRequestDto();
        request.setDescription("Consult");
        request.setStatus("AVAILABLE");
        request.setStartTime(slot.getStartTime());
        request.setEndTime(slot.getEndTime());

        when(userRepository.findByUsername("provider1")).thenReturn(Optional.of(provider));
        when(slotRepository.findByProvider(provider)).thenReturn(List.of());
        when(slotRepository.save(any(Slot.class))).thenReturn(slot);
        when(config.getMinSlotDurationMinutes()).thenReturn(30);
        when(config.getMaxSlotDurationMinutes()).thenReturn(180);
        SlotResponseDto response = slotService.addSlot(request, "provider1");

        assertEquals("Consult", response.getDescription());
        assertEquals("AVAILABLE", response.getStatus());
        assertEquals(10L, response.getId());
        assertEquals("Cardiology", response.getProviderSpecialization());
    }

    @Test
    void testAddSlotUserNotFound() {
        when(userRepository.findByUsername("provider1")).thenReturn(Optional.empty());
        SlotRequestDto request = new SlotRequestDto();

        assertThrows(UsernameNotFoundException.class, () -> slotService.addSlot(request, "provider1"));
    }

    @Test
    void testAddSlotClash() {
        SlotRequestDto request = new SlotRequestDto();
        request.setDescription("Consult");
        request.setStatus("AVAILABLE");
        request.setStartTime(slot.getStartTime());
        request.setEndTime(slot.getEndTime());

        when(userRepository.findByUsername("provider1")).thenReturn(Optional.of(provider));
        when(slotRepository.findByProvider(provider)).thenReturn(List.of(slot));

        assertThrows(IllegalStateException.class, () -> slotService.addSlot(request, "provider1"));
    }

    @Test
    void testAddSlotInvalidDuration() {
        SlotRequestDto request = new SlotRequestDto();
        request.setDescription("Consult");
        request.setStatus("AVAILABLE");
        request.setStartTime(slot.getStartTime());
        request.setEndTime(slot.getStartTime().plusMinutes(10)); // too short

        when(userRepository.findByUsername("provider1")).thenReturn(Optional.of(provider));
        when(slotRepository.findByProvider(provider)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> slotService.addSlot(request, "provider1"));
    }

    @Test
    void testGetSlotsForProviderSuccess() {
        when(userRepository.findByUsername("provider1")).thenReturn(Optional.of(provider));
        when(slotRepository.findByProviderId(1L)).thenReturn(List.of(slot));

        List<SlotResponseDto> slots = slotService.getSlotsForProvider("provider1");

        assertEquals(1, slots.size());
        assertEquals("Consult", slots.get(0).getDescription());
        assertEquals("Cardiology", slots.get(0).getProviderSpecialization());
    }

    @Test
    void testGetSlotsForProvider_UserNotFound() {
        when(userRepository.findByUsername("provider1")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> slotService.getSlotsForProvider("provider1"));
    }

    @Test
    void testGetAllSlots() {
        when(slotRepository.findAll()).thenReturn(List.of(slot));

        List<SlotResponseDto> slots = slotService.getAllSlots();

        assertEquals(1, slots.size());
        assertEquals("Cardiology", slots.get(0).getProviderSpecialization());
    }

    @Test
    void testGetByIdFound() {
        when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));

        Optional<SlotResponseDto> result = slotService.getById(10L);

        assertTrue(result.isPresent());
        assertEquals("Consult", result.get().getDescription());
        assertEquals("Cardiology", result.get().getProviderSpecialization());
    }

    @Test
    void testGetByIdNotFound() {
        when(slotRepository.findById(99L)).thenReturn(Optional.empty());
        assertTrue(slotService.getById(99L).isEmpty());
    }

    @Test
    void testDeleteSlotSuccess() {
        when(slotRepository.existsById(10L)).thenReturn(true);
        when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));
        when(userRepository.findByUsername("provider1")).thenReturn(Optional.of(provider));

        String message = slotService.deleteSlot(10L, "provider1");

        assertEquals("Slot with id: 10 deleted successfully", message);
        verify(slotRepository).deleteById(10L);
    }

    @Test
    void testDeleteSlotNotFound() {
        when(slotRepository.existsById(10L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> slotService.deleteSlot(10L, "provider1"));
    }

    @Test
    void testDeleteSlotUnauthorized() {
        User otherProvider = User.builder().id(2L).username("other").role("PROVIDER").build();
        when(slotRepository.existsById(10L)).thenReturn(true);
        when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherProvider));

        assertThrows(ResponseStatusException.class, () -> slotService.deleteSlot(10L, "other"));
    }

    @Test
    void testUpdateSlotSuccess() {
        SlotRequestDto request = new SlotRequestDto();
        request.setDescription("Updated");
        request.setStartTime(slot.getStartTime().plusHours(1));
        request.setEndTime(slot.getEndTime().plusHours(1));

        when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));
        when(userRepository.findByUsername("provider1")).thenReturn(Optional.of(provider));
        when(slotRepository.findByProviderUsername("provider1")).thenReturn(List.of(slot));
        when(slotRepository.save(any(Slot.class))).thenReturn(slot);

        SlotResponseDto updated = slotService.updateSlot(10L, "provider1", request);

        assertEquals("Updated", updated.getDescription());
        assertEquals("Cardiology", updated.getProviderSpecialization());
    }

    @Test
    void testUpdateSlotExpired() {
        slot.setStatus(SlotStatus.EXPIRED);
        when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));

        SlotRequestDto request = new SlotRequestDto();

        assertThrows(ResponseStatusException.class, () -> slotService.updateSlot(10L, "provider1", request));
    }

    @Test
    void testUpdateSlotUnauthorized() {
        when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));
        User otherProvider = User.builder().id(2L).username("other").role("PROVIDER").build();
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherProvider));

        SlotRequestDto request = new SlotRequestDto();
        assertThrows(ResponseStatusException.class, () -> slotService.updateSlot(10L, "other", request));
    }

    @Test
    void testUpdateSlotClash() {
        SlotRequestDto request = new SlotRequestDto();
        request.setDescription("Clashing slot");
        request.setStartTime(slot.getStartTime());
        request.setEndTime(slot.getEndTime());

        when(slotRepository.findById(10L)).thenReturn(Optional.of(slot));
        when(userRepository.findByUsername("provider1")).thenReturn(Optional.of(provider));

        Slot otherSlot = new Slot();
        otherSlot.setId(20L);
        otherSlot.setStartTime(slot.getStartTime());
        otherSlot.setEndTime(slot.getEndTime());

        when(slotRepository.findByProviderUsername("provider1")).thenReturn(List.of(otherSlot));

        assertThrows(IllegalStateException.class, () -> slotService.updateSlot(10L, "provider1", request));
    }




    @Test
    void testGetSlotsForProviderWhenNoSlots() {
        when(userRepository.findByUsername("provider1")).thenReturn(Optional.of(provider));
        when(slotRepository.findByProviderId(1L)).thenReturn(Collections.emptyList());

        List<SlotResponseDto> slots = slotService.getSlotsForProvider("provider1");
        assertTrue(slots.isEmpty());
    }

    @Test
    void testUpdateSlotNotFound() {
        when(slotRepository.findById(99L)).thenReturn(Optional.empty());
        SlotRequestDto request = new SlotRequestDto();

        assertThrows(ResourceNotFoundException.class, () -> slotService.updateSlot(99L, "provider1", request));
    }


}