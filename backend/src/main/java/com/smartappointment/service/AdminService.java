package com.smartappointment.service;

import com.smartappointment.dto.AppointmentResponseDto;
import com.smartappointment.entity.Appointment;
import com.smartappointment.entity.CancellationLog;
import com.smartappointment.entity.User;
import com.smartappointment.repository.AppointmentRepository;
import com.smartappointment.repository.CancellationLogRepository;
import com.smartappointment.repository.UserRepository;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AdminService {
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CancellationLogRepository cancellationLogRepository;

    //No of Appointments per provider
    public long getTotalAppointmentsForProvider(Long providerId) {
        log.info("Getting appointment");
        if (!userRepository.existsById(providerId)) {
            throw new ResourceNotFoundException("Provider with id " + providerId + " not found.");
        }
        if(!userRepository.findById(providerId).get().getRole().equals("PROVIDER")){
            throw new ResourceNotFoundException("Provider with id " + providerId + " not found.");
        }
        return appointmentRepository.findBySlotProviderId(providerId).size();
    }

    //Appointment list for provider
    public List<AppointmentResponseDto> getProviderAppointments(Long providerId){
        if (!userRepository.existsById(providerId)) {
            throw new ResourceNotFoundException("Provider with id " + providerId + " not found.");
        }
        if(!userRepository.findById(providerId).get().getRole().equals("PROVIDER")){
            throw new ResourceNotFoundException("Provider with id " + providerId + " not found.");
        }

        List<Appointment> appointments=appointmentRepository.findBySlotProviderId(providerId);

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

    public long getCancellationsForProvider(Long providerId) {
        if (!userRepository.existsById(providerId)) {
            throw new ResourceNotFoundException("Provider with id " + providerId + " not found.");
        }
        if(!userRepository.findById(providerId).get().getRole().equals("PROVIDER")){
            throw new ResourceNotFoundException("Provider with id " + providerId + " not found.");
        }
        return cancellationLogRepository.findByProviderId(providerId).size();
    }

    public Map<Integer, Long>  getPeakBookingHours() {
        log.info("getting peak hrs");
        List<Appointment> appointments = appointmentRepository.findAll();

        return appointments.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getSlot().getStartTime().getHour(),
                        Collectors.counting()
                ));
    }

    public double getCancellationRateForProvider(Long providerId) {
        if (!userRepository.existsById(providerId)) {
            throw new ResourceNotFoundException("Provider with id " + providerId + " not found.");
        }
        if(!userRepository.findById(providerId).get().getRole().equals("PROVIDER")){
            throw new ResourceNotFoundException("Provider with id " + providerId + " not found.");
        }


        long totalCancellations = cancellationLogRepository.findByProviderId(providerId).size();
        long totalAppointments = totalCancellations + appointmentRepository.findBySlotProviderId(providerId).size();

        if (totalAppointments == 0) return 0.0;

        return ((double) totalCancellations / totalAppointments) * 100;
    }


}



