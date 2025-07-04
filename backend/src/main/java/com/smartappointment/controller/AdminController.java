package com.smartappointment.controller;

import com.smartappointment.dto.AppointmentResponseDto;
import com.smartappointment.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // Get total appointments for a provider
    @GetMapping("/appointments/count/{providerId}")
    public ResponseEntity<Long> getTotalAppointments(@PathVariable Long providerId) {
        long count = adminService.getTotalAppointmentsForProvider(providerId);
        return ResponseEntity.ok(count);
    }

    //appointment list for a provider
    @GetMapping("/appointments/getAppointmentList/{providerId}")
    public ResponseEntity<List<AppointmentResponseDto>> getAppointmentList(@PathVariable Long providerId){
        log.info("getting appointments");
        List<AppointmentResponseDto> response=adminService.getProviderAppointments(providerId);
        return ResponseEntity.ok(response);
    }

    // Get total cancellations for a provider
    @GetMapping("/cancellations/count/{providerId}")
    public ResponseEntity<Long> getTotalCancellations(@PathVariable Long providerId) {
        long cancellations = adminService.getCancellationsForProvider(providerId);
        return ResponseEntity.ok(cancellations);
    }

    // Get peak booking hours
    @GetMapping("/appointments/peak-hours")
    public ResponseEntity<Map<Integer, Long>> getPeakBookingHours() {
        log.info("getting peak hrs");

        Map<Integer, Long> peakHours = adminService.getPeakBookingHours();
        return ResponseEntity.ok(peakHours);
    }

    //Cancellation rates per provider
    @GetMapping("/cancellations/rate/{providerId}")
    public ResponseEntity<Double> getCancellationRate(@PathVariable Long providerId) {
        double rate = adminService.getCancellationRateForProvider(providerId);
        return ResponseEntity.ok(rate);
    }

}
