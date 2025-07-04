package com.smartappointment.service;


import com.smartappointment.config.AppConfigProperties;
import com.smartappointment.dto.AppointmentResponseDto;
import com.smartappointment.entity.Appointment;
import com.smartappointment.entity.CancellationLog;
import com.smartappointment.entity.Slot;
import com.smartappointment.entity.User;
import com.smartappointment.notification.NotificationService;
import com.smartappointment.notification.kafka.producer.KafkaProducerService;
import com.smartappointment.repository.AppointmentRepository;
import com.smartappointment.repository.CancellationLogRepository;
import com.smartappointment.repository.SlotRepository;
import com.smartappointment.repository.UserRepository;
import com.smartappointment.util.enumerations.AppointmentStatus;
import com.smartappointment.util.enumerations.SlotStatus;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import io.micrometer.core.instrument.MeterRegistry;


import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private CancellationLogRepository cancellationLogRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private AppConfigProperties config;
    @Autowired
    private KafkaProducerService kafkaProducerService;
    @Autowired
    private QueueService queueService;

    //get user appointments
    public List<AppointmentResponseDto> getUserAppointments(String username){
        List<Appointment> appointments=appointmentRepository.findByUserUsername(username);

        return   appointments
                .stream()
                .map(appointment -> AppointmentResponseDto.builder()
                        .appointmentId(appointment.getId())
                        .slotId(appointment.getSlot().getId())
                        .description(appointment.getSlot().getDescription())
                        .startTime(appointment.getSlot().getStartTime())
                        .endTime(appointment.getSlot().getEndTime())
                        .providerId(appointment.getSlot().getProvider().getId())
                        .providerUsername(appointment.getSlot().getProvider().getUsername())
                        .providerEmail(appointment.getSlot().getProvider().getEmail())
                        .status(String.valueOf(appointment.getStatus()))
                        .build())
                .toList();
    }

    //get provider appointments
    public List<AppointmentResponseDto> getProviderAppointments(String username){
        List<Appointment> appointments=appointmentRepository.findBySlotProviderUsername(username);

        return   appointments
                .stream()
                .map(appointment -> AppointmentResponseDto.builder()
                        .appointmentId(appointment.getId())
                        .slotId(appointment.getSlot().getId())
                        .description(appointment.getSlot().getDescription())
                        .startTime(appointment.getSlot().getStartTime())
                        .endTime(appointment.getSlot().getEndTime())
                        .providerId(appointment.getSlot().getProvider().getId())
                        .providerUsername(appointment.getSlot().getProvider().getUsername())
                        .providerEmail(appointment.getSlot().getProvider().getEmail())
                        .status(String.valueOf(appointment.getStatus()))
                        .userUsername(appointment.getUser().getUsername())
                        .userEmail(appointment.getUser().getEmail())
                        .build())
                .toList();
    }

    //get appointments by id
    public AppointmentResponseDto getById(Long appointmentId,String username){
        Appointment appointment=appointmentRepository.findById(appointmentId). orElseThrow(()->new EntityNotFoundException("Appointment with id: "+appointmentId+" not found"));
        //check if username matches with the user/provider of the appointment
        if(!appointment.getUser().getUsername().equals(username)&&!appointment.getSlot().getProvider().getUsername().equals(username)){
            throw new EntityNotFoundException("Appointment with id "+appointmentId+" not found for username: "+ username);
        }
        return AppointmentResponseDto.builder()
                .appointmentId(appointment.getId())
                .providerUsername(appointment.getSlot().getProvider().getUsername())
                .startTime(appointment.getSlot().getStartTime())
                .endTime(appointment.getSlot().getEndTime())
                .providerId(appointment.getSlot().getProvider().getId())
                .providerEmail(appointment.getSlot().getProvider().getEmail())
                .slotId(appointment.getSlot().getId())
                .description(appointment.getSlot().getDescription())
                .status(String.valueOf(appointment.getStatus()))
                .userUsername(appointment.getUser().getUsername())
                .build();
    }

    //book appointment
    @Transactional
    public AppointmentResponseDto createAppointment(Long slotId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new EntityNotFoundException("Slot not found"));

        if (!slot.getStatus().equals(SlotStatus.AVAILABLE)) {
            throw new IllegalStateException("Slot is either booked or expired");
        }

        //check for max bookings per day
        LocalDateTime startOfDay = slot.getStartTime().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        long dailyBookingCount = appointmentRepository.findByUser(user).stream()
                .filter(appointment -> {
                    LocalDateTime appointmentTime = appointment.getSlot().getStartTime();
                    return appointmentTime.isAfter(startOfDay) && appointmentTime.isBefore(endOfDay);
                })
                .count();

        if (dailyBookingCount >= config.getMaxBookingsPerDay()) {
            throw new IllegalStateException("You have reached the maximum number of bookings for today");
        }

        // check for clashing appointments
        List<Appointment> existingAppointments = appointmentRepository.findByUser(user);

        boolean hasClash = existingAppointments.stream().anyMatch(appointment ->
                appointment.getSlot().getStartTime().isBefore(slot.getEndTime())
                        && slot.getStartTime().isBefore(appointment.getSlot().getEndTime())
        );

        if (hasClash) {
            throw new IllegalStateException("You already have an appointment that overlaps with this slot");
        }

        // mark slot as unavailable
        slot.setStatus(SlotStatus.BOOKED);

        // create new appointment
        Appointment appointment = Appointment.builder()
                .slot(slot)
                .user(user)
                .status(AppointmentStatus.BOOKED)
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);

        meterRegistry.counter("appointments.booked.count").increment();
        notificationService.sendNotification(appointment.getUser().getEmail(), "Your appointment is booked successfully!");
        kafkaProducerService.sendNotification("Your appointment is booked successfully!");

        return AppointmentResponseDto.builder()
                .appointmentId(savedAppointment.getId())
                .slotId(slot.getId())
                .description(slot.getDescription())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .providerId(slot.getProvider().getId())
                .status("AVAILABLE")
                .providerUsername(slot.getProvider().getUsername())
                .providerEmail(slot.getProvider().getEmail())
                .userUsername(savedAppointment.getUser().getUsername())
                .build();
    }



    //cancel appointment
    @Transactional
    public void deleteAppointment(Long appointmentId, String username) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        // Only allow the user who booked or the provider or admin to delete
        if (!appointment.getUser().getUsername().equals(username) &&
                !appointment.getSlot().getProvider().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete this appointment");
        }

        if(appointment.getStatus().equals(AppointmentStatus.EXPIRED)||appointment.getStatus().equals(AppointmentStatus.COMPLETED)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This appointment is either expired or completed");
        }

        Slot slot=appointment.getSlot();
        slot.setStatus(SlotStatus.AVAILABLE);
        slotRepository.save(slot);
        CancellationLog cancellationLoglog = new CancellationLog();
        cancellationLoglog.setAppointmentId(appointment.getId());
        cancellationLoglog.setSlotId(slot.getId());
        cancellationLoglog.setProviderId(slot.getProvider().getId());
        cancellationLoglog.setUserId(appointment.getUser().getId());
        cancellationLoglog.setCancelledAt(LocalDateTime.now());
        cancellationLogRepository.save(cancellationLoglog);

        log.info("Deleting appointment");
        appointmentRepository.delete(appointment);
        appointmentRepository.flush();
        log.info("auto booking for next user in queue");
        queueService.dequeueNextAndBook(appointment.getSlot().getId());
        log.info("auto book successful");
        meterRegistry.counter("appointments.cancelled.count").increment();
        notificationService.sendNotification(appointment.getUser().getEmail(), "Your appointment is cancelled successfully!");
        kafkaProducerService.sendNotification("User: " + appointment.getUser().getEmail()+"Your appointment is cancelled successfully!");
    }

    //complete appointment
    @Transactional
    public void completeAppointment(Long appointmentId, String username) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        if(!appointment.getSlot().getProvider().getUsername().equals(username)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You have no appointment with id: "+appointmentId);
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(appointment.getSlot().getStartTime()) || now.isAfter(appointment.getSlot().getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment can only be completed during its scheduled time.");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.getSlot().setStatus(SlotStatus.EXPIRED);
        slotRepository.save(appointment.getSlot());
        appointmentRepository.save(appointment);
        notificationService.sendNotification(appointment.getUser().getEmail(), "Your appointment is completed successfully!");
        kafkaProducerService.sendNotification("User: "+appointment.getUser().getEmail()+" : Your appointment is completed successfully!");
    }
}