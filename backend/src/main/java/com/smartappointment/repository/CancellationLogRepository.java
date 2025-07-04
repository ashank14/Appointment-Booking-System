package com.smartappointment.repository;

import com.smartappointment.entity.CancellationLog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CancellationLogRepository extends JpaRepository<CancellationLog, Long> {
    List<CancellationLog> findByProviderId(Long providerId);
}
