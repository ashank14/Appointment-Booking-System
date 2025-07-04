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
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SlotService {

    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AppConfigProperties config;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private KafkaProducerService kafkaProducerService;
    @Autowired
    private QueueService queueService;



    //add/create slot
    @Transactional
    public SlotResponseDto addSlot(SlotRequestDto requestDto, String username) {
        User provider = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // check for clashing slots for this provider
        List<Slot> existingSlots = slotRepository.findByProvider(provider)
                .stream()
                .filter(slot -> slot.getStatus() == SlotStatus.AVAILABLE || slot.getStatus() == SlotStatus.BOOKED)
                .toList();
        boolean hasClash = existingSlots.stream().anyMatch(existingSlot ->
                existingSlot.getStartTime().isBefore(requestDto.getEndTime())
                        && requestDto.getStartTime().isBefore(existingSlot.getEndTime())
        );

        if (hasClash) {
            throw new IllegalStateException("This provider already has a slot that overlaps with the given time range.");
        }

        // validate slot duration
        long durationMinutes = java.time.Duration.between(requestDto.getStartTime(), requestDto.getEndTime()).toMinutes();
        if (durationMinutes < config.getMinSlotDurationMinutes() || durationMinutes > config.getMaxSlotDurationMinutes()) {
            throw new IllegalStateException("Slot duration must be between " + config.getMinSlotDurationMinutes()
                    + " and " + config.getMaxSlotDurationMinutes() + " minutes.");
        }
        // no clash — create slot
        Slot slot = new Slot();
        slot.setDescription(requestDto.getDescription());
        slot.setStatus(SlotStatus.valueOf(requestDto.getStatus()));
        slot.setStartTime(requestDto.getStartTime());
        slot.setEndTime(requestDto.getEndTime());
        slot.setProvider(provider);

        Slot savedSlot = slotRepository.save(slot);

        return SlotResponseDto.builder()
                .id(savedSlot.getId())
                .description(savedSlot.getDescription())
                .startTime(savedSlot.getStartTime())
                .endTime(savedSlot.getEndTime())
                .status(savedSlot.getStatus().name())
                .providerId(savedSlot.getProvider().getId())
                .providerUsername(savedSlot.getProvider().getUsername())
                .providerEmail(savedSlot.getProvider().getEmail())
                .providerSpecialization(savedSlot.getProvider().getSpecialization())
                .build();
    }


    //get slots for a provider
    public List<SlotResponseDto> getSlotsForProvider(String username) {
        User provider = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return slotRepository.findByProviderId(provider.getId())
                .stream()
                .map(slot -> SlotResponseDto.builder()
                        .id(slot.getId())
                        .description(slot.getDescription())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .providerId(slot.getProvider().getId())
                        .status(slot.getStatus().name())
                        .providerUsername(slot.getProvider().getUsername())
                        .providerEmail(slot.getProvider().getEmail())
                        .providerSpecialization(slot.getProvider().getSpecialization())
                        .build())
                .toList();
    }

    //get all slots
    public List<SlotResponseDto> getAllSlots() {
        List<Slot> slots = slotRepository.findAll();

        return slots
                .stream()
                .map(slot -> SlotResponseDto.builder()
                        .id(slot.getId())
                        .description(slot.getDescription())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .providerId(slot.getProvider().getId())
                        .status(slot.getStatus().name())
                        .providerUsername(slot.getProvider().getUsername())
                        .providerEmail(slot.getProvider().getEmail())
                        .providerSpecialization(slot.getProvider().getSpecialization())
                        .build())
                .toList();
    }

    //get slot by id
    public Optional<SlotResponseDto> getById(Long id) {
        return slotRepository.findById(id)
                .map(slot -> new SlotResponseDto(
                        slot.getId(),
                        slot.getDescription(),
                        slot.getStartTime(),
                        slot.getEndTime(),
                        slot.getStatus().name(),
                        slot.getProvider().getId(),
                        slot.getProvider().getUsername(),
                        slot.getProvider().getEmail(),
                        slot.getProvider().getSpecialization()
                ));
    }

    //delete slot
    @Transactional
    public String deleteSlot(Long id,String username){
        if (!slotRepository.existsById(id)) {
            throw new ResourceNotFoundException("Slot not found with id: " + id);
        }
        Slot slot = slotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with id: " + id));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id"));

        if (!slot.getProvider().getId().equals(user.getId()) && !user.getRole().equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete this slot");
        }

        slotRepository.deleteById(id);
        return "Slot with id: "+ id +" deleted successfully";
    }


    //update
    @Transactional
    public SlotResponseDto updateSlot(Long id, String username, SlotRequestDto request) {
        Slot slot = slotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with id: " + id));

        if(slot.getStatus().equals(SlotStatus.EXPIRED)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This slot is expired");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        if (!slot.getProvider().getId().equals(user.getId()) && !user.getRole().equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to update this slot");
        }

        // Fetch all other slots for this provider (excluding current slot)
        List<Slot> otherSlots = slotRepository.findByProviderUsername(user.getUsername()).stream()
                .filter(s -> !s.getId().equals(id))
                .toList();

        // Check for clashing slots
        boolean hasClash = otherSlots.stream().anyMatch(existingSlot ->
                existingSlot.getStartTime().isBefore(request.getEndTime()) &&
                        request.getStartTime().isBefore(existingSlot.getEndTime())
        );

        if (hasClash) {
            throw new IllegalStateException("This update causes a time clash with an existing slot.");
        }

        // Update slot details
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setDescription(request.getDescription());

        Slot updatedSlot = slotRepository.save(slot);

        queueService.clearQueueForSlot(id);

        notificationService.sendNotification("","Slot: "+ id +" has been rescheduled....Please join the queue again");
        kafkaProducerService.sendNotification("Slot: "+ id +" has been rescheduled....Please join the queue again");

        return SlotResponseDto.builder()
                .id(updatedSlot.getId())
                .description(updatedSlot.getDescription())
                .startTime(updatedSlot.getStartTime())
                .endTime(updatedSlot.getEndTime())
                .status(updatedSlot.getStatus().name())
                .providerId(updatedSlot.getProvider().getId())
                .providerUsername(updatedSlot.getProvider().getUsername())
                .providerEmail(updatedSlot.getProvider().getEmail())
                .providerSpecialization(updatedSlot.getProvider().getSpecialization())
                .build();
    }
}

