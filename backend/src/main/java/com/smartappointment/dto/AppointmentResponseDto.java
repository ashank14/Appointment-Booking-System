package com.smartappointment.dto;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppointmentResponseDto {
    private Long appointmentId;
    private Long slotId;
    private String description;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String userId;
    private String userUsername;
    private String userEmail;
    private Long providerId;
    private String providerUsername;
    private String providerEmail;
}
