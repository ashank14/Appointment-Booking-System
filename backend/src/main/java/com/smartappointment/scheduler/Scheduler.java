package com.smartappointment.scheduler;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.smartappointment.entity.Appointment;
import com.smartappointment.entity.Slot;
import com.smartappointment.notification.NotificationService;
import com.smartappointment.notification.kafka.producer.KafkaProducerService;
import com.smartappointment.repository.AppointmentRepository;
import com.smartappointment.repository.SlotRepository;
import com.smartappointment.service.QueueService;
import com.smartappointment.util.enumerations.AppointmentStatus;
import com.smartappointment.util.enumerations.SlotStatus;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class Scheduler {
    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private QueueService queueService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void updateExpiredAppointments() {
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> pastAppointments = appointmentRepository.findBySlotEndTimeBeforeAndStatus(now, AppointmentStatus.BOOKED);

        for (Appointment appointment : pastAppointments) {
            appointment.setStatus(AppointmentStatus.EXPIRED); // or MISSED if no-show logic
            appointmentRepository.save(appointment);
            kafkaProducerService.sendNotification("Appointment: "+appointment.getId()+" expired!!");
            notificationService.sendNotification(appointment.getUser().getUsername(),"Appointment: "+ appointment.getId()+" expired");
            // expire the booked slot
            Slot slot = appointment.getSlot();
            slot.setStatus(SlotStatus.EXPIRED);


            queueService.clearQueueForSlot(slot.getId());
            slotRepository.save(slot);
            notificationService.sendNotification(slot.getProvider().getUsername(),"Slot: "+ slot.getId()+" expired");
            kafkaProducerService.sendNotification("Slot: "+slot.getId()+" expired!!");
        }
    }


    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireOldSlots() {
        LocalDateTime now = LocalDateTime.now();
        System.out.println("scheduler");
        List<Slot> oldSlots = slotRepository.findByEndTimeBeforeAndStatus(now, SlotStatus.AVAILABLE);
        for (Slot s : oldSlots) {
            s.setStatus(SlotStatus.EXPIRED);
            queueService.clearQueueForSlot(s.getId());
            slotRepository.save(s);
            notificationService.sendNotification(s.getProvider().getUsername(),"Slot: "+ s.getId()+" expired");
            kafkaProducerService.sendNotification("Slot: "+s.getId()+" expired!!");

        }
    }

    @Scheduled(fixedRate = 60000)
    public void sendAppointmentReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(5);

        List<Appointment> upcomingAppointments = appointmentRepository
                .findAll()
                .stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.BOOKED)
                .filter(appointment -> appointment.getSlot().getStartTime().isAfter(now))
                .filter(appointment -> appointment.getSlot().getStartTime().isBefore(reminderTime))
                .toList();

        for (Appointment appointment : upcomingAppointments) {
            notificationService.sendAppointmentReminder(appointment);
            kafkaProducerService.sendNotification("Reminder: Appointment with ID: "+ appointment.getId() +" for user: " + appointment.getUser().getUsername() +
                    "starts at"+
                    appointment.getSlot().getStartTime());
        }
    }
}
