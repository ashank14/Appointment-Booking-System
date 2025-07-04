package com.smartappointment.repository;

import com.smartappointment.entity.Slot;
import com.smartappointment.entity.User;
import com.smartappointment.util.enumerations.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    List<Slot> findByProviderAndStatus(User provider,SlotStatus status);
    List<Slot> findByProvider(User provider);
    List<Slot> findByProviderUsername(String provider);
    List<Slot> findByProviderId(Long providerId);
    List<Slot> findByEndTimeBeforeAndStatus(LocalDateTime now, SlotStatus status);

}
