package com.smartappointment.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SlotResponseDto {
    private Long id;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Long providerId;
    private String providerUsername;
    private String providerEmail;
    private String providerSpecialization;
}