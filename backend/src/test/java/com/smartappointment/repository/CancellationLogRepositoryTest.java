package com.smartappointment.repository;

import com.smartappointment.entity.CancellationLog;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
class CancellationLogRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

    }
    @BeforeAll
    static void checkContainerRunning() {
        assertTrue(postgresContainer.isRunning());
    }

    @Autowired
    private CancellationLogRepository cancellationLogRepository;

    @Test
    void testFindByProviderId() {
        CancellationLog log = CancellationLog.builder()
                .appointmentId(1L)
                .slotId(2L)
                .providerId(3L)
                .userId(4L)
                .cancelledAt(LocalDateTime.now())
                .build();

        cancellationLogRepository.save(log);

        List<CancellationLog> logs = cancellationLogRepository.findByProviderId(3L);
        assertEquals(1, logs.size());
        assertEquals(1L, logs.get(0).getAppointmentId());
    }
}
