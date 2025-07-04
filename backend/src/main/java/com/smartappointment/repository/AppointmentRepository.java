package com.smartappointment.repository;

import com.smartappointment.entity.Appointment;
import com.smartappointment.entity.User;
import com.smartappointment.util.enumerations.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment,Long> {
    // Get all appointments for a given user by User object
    List<Appointment> findByUser(User user);

    List<Appointment> findByUserUsername(String username);
    List<Appointment> findBySlotProviderUsername(String username);
    List<Appointment> findBySlotEndTimeBeforeAndStatus(LocalDateTime now, AppointmentStatus status);
    
    // Get all appointments for a specific provider by providerId
    List<Appointment> findBySlotProviderId(Long providerId);

    // Get appointments for a specific slot
    Appointment findBySlotId(Long slotId);
}
