package com.smartappointment.controller;


import com.smartappointment.dto.AppointmentResponseDto;
import com.smartappointment.service.AppointmentService;
import com.smartappointment.service.SlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;
    //get appointments by user id
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/getUserAppointments")
    public ResponseEntity<List<AppointmentResponseDto>> getUserAppointments(Authentication authentication){
        String username= authentication.getName();
        List<AppointmentResponseDto> response=appointmentService.getUserAppointments(username);
        return ResponseEntity.ok(response);
    }

    //get appointments by provider id
    @PreAuthorize("hasRole('PROVIDER')")
    @GetMapping("/getProviderAppointments")
    public ResponseEntity<List<AppointmentResponseDto>> getProviderAppointments(Authentication authentication){
        String username= authentication.getName();
        List<AppointmentResponseDto> response=appointmentService.getProviderAppointments(username);
        return ResponseEntity.ok(response);
    }

    //Book a slot
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/createAppointment/{slotId}")
    public ResponseEntity<AppointmentResponseDto> createAppointment(@PathVariable Long slotId, Authentication authentication){
        AppointmentResponseDto appointment=appointmentService.createAppointment(slotId,authentication.getName());
        return ResponseEntity.ok(appointment);
    }

    //cancel appointment
    @DeleteMapping("/cancel/{appointmentId}")
    public ResponseEntity<String> deleteAppointment(@PathVariable Long appointmentId, Authentication authentication) {
        String username = authentication.getName();
        appointmentService.deleteAppointment(appointmentId, username);
        return ResponseEntity.ok("Appointment deleted successfully.");
    }

    // GET appointment by ID
    @GetMapping("/getById/{appointmentId}")
    public ResponseEntity<AppointmentResponseDto> getAppointmentById(@PathVariable Long appointmentId, Authentication authentication) {
        String username = authentication.getName();
        AppointmentResponseDto response = appointmentService.getById(appointmentId, username);
        return ResponseEntity.ok(response);
    }
    //complete appointment
    @PreAuthorize("hasRole('PROVIDER')")
    @PutMapping("/completeAppointment/{appointmentId}")
    public ResponseEntity<String> completeAppointment(@PathVariable Long appointmentId, Authentication authentication){
        String username = authentication.getName();
        appointmentService.completeAppointment(appointmentId,username);
        return ResponseEntity.ok("Appointment : "+ appointmentId+" marked as completed");

    }

}
