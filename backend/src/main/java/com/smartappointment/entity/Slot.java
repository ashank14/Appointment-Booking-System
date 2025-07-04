package com.smartappointment.entity;

import com.smartappointment.util.enumerations.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import com.smartappointment.util.enumerations.SlotStatus;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Slot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @Enumerated(EnumType.STRING)
    private SlotStatus status;
    @ManyToOne
    @JoinColumn(name="provider_id")
    private User provider;

}
