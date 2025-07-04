package com.smartappointment.notification;

import com.smartappointment.entity.Appointment;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    @Autowired
    private MeterRegistry meterRegistry;
    @Async
    public void sendNotification(String to, String message) {
        log.info("Sending notification to: {} on thread: {}", to, Thread.currentThread().getName());
        // Simulate delay
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Notification sent to {}: {}", to, message);
        meterRegistry.counter("notifications.sent.count").increment();

    }

    @Async
    public void sendAppointmentReminder(Appointment appointment) {
        log.info("Reminder: Appointment with ID {} for user {} starts at {}.",
                appointment.getId(),
                appointment.getUser().getUsername(),
                appointment.getSlot().getStartTime());
        meterRegistry.counter("reminders.sent.count").increment();
    }

}
