package com.smartappointment.service;

import com.smartappointment.entity.Appointment;
import com.smartappointment.entity.Slot;
import com.smartappointment.entity.User;
import com.smartappointment.repository.AppointmentRepository;
import com.smartappointment.repository.SlotRepository;
import com.smartappointment.util.enumerations.AppointmentStatus;
import com.smartappointment.util.enumerations.SlotStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final RedisTemplate<String, User> redisTemplate;
    private final AppointmentRepository appointmentRepository;
    private final SlotRepository slotRepository;

    private String getQueueKey(Long slotId) {
        return "queue:slot:" + slotId;
    }

    // Join queue
    public void joinQueue(Long slotId, User user) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (slot.getStatus().equals(SlotStatus.AVAILABLE) || LocalDateTime.now().isAfter(slot.getEndTime())) {
            throw new RuntimeException("Cannot queue for an available or expired slot");
        }

        Appointment appt = appointmentRepository.findBySlotId(slot.getId());
        if (appt != null && appt.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Cannot queue for your own slot");
        }

        // check for clashing appointments
        List<Appointment> existingAppointments = appointmentRepository.findByUser(user);

        boolean hasClash = existingAppointments.stream().anyMatch(appointment ->
                appointment.getSlot().getStartTime().isBefore(slot.getEndTime())
                        && slot.getStartTime().isBefore(appointment.getSlot().getEndTime())
        );

        if (hasClash) {
            throw new RuntimeException("You already have a booked appointment in this time slot");
        }

        // Check queue clash
        Set<String> keysSet = redisTemplate.keys("queue:slot:*");
        List<String> keys = new ArrayList<>(keysSet);

        for (String key : keys) {
            List<User> queuedUsers = redisTemplate.opsForList().range(key, 0, -1);
            for (User u : queuedUsers) {
                Slot otherSlot = slotRepository.findById(Long.parseLong(key.split(":")[2])).get();
                if (u.getId().equals(user.getId())
                        && timeOverlap(otherSlot.getStartTime(), otherSlot.getEndTime(), slot.getStartTime(), slot.getEndTime())) {
                    throw new RuntimeException("You’re already queued for a slot in the same time window");
                }
            }
        }

        // Add to Redis list (queue)
        redisTemplate.opsForList().rightPush(getQueueKey(slotId), user);
        log.info("User {} joined queue for slot {}", user.getUsername(), slotId);
    }

    public List<User> getQueue(Long slotId) {
        return redisTemplate.opsForList().range(getQueueKey(slotId), 0, -1);
    }

    public void removeQueue(Long slotId) {
        redisTemplate.delete(getQueueKey(slotId));
    }

    public void dequeueNextAndBook(Long slotId) {
        User nextUser = redisTemplate.opsForList().leftPop(getQueueKey(slotId));
        if (nextUser != null) {
            // Book for this user
            log.info("Auto-booking slot {} for next user in queue: {}", slotId, nextUser.getUsername());
            Appointment appointment = new Appointment();
            appointment.setSlot(slotRepository.findById(slotId).get());
            appointment.setUser(nextUser);
            appointment.setStatus(AppointmentStatus.BOOKED);
            slotRepository.findById(appointment.getSlot().getId()).get().setStatus(SlotStatus.BOOKED);
            log.info(appointment.toString());
            appointmentRepository.save(appointment);
        }
    }

    public void clearQueueForSlot(Long slotId) {
        redisTemplate.delete("queue:slot:" + slotId);
    }

    public void leaveQueue(Long slotId, User user) {
        String queueKey = "queue:slot:" + slotId;

        // Check if queue exists
        if (!redisTemplate.hasKey(queueKey)) {
            throw new RuntimeException("No queue exists for this slot.");
        }

        // Fetch current queue entries
        List<User> queue = redisTemplate.opsForList().range(queueKey, 0, -1);
        if (queue == null || queue.isEmpty()) {
            throw new RuntimeException("You are not in the queue for this slot.");
        }

        // Check if user is in queue
        boolean inQueue = queue.stream().anyMatch(u -> u.getId().equals(user.getId()));
        if (!inQueue) {
            throw new RuntimeException("You are not in the queue for this slot.");
        }

        // Remove user from queue by re-adding others (since Redis doesn't support direct list item removal)
        queue.removeIf(u -> u.getId().equals(user.getId()));

        // Delete old queue and replace with updated list
        redisTemplate.delete(queueKey);
        for (User u : queue) {
            redisTemplate.opsForList().rightPush(queueKey, u);
        }
    }

    private boolean timeOverlap(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return (start1.isBefore(end2) && start2.isBefore(end1));
    }
}
