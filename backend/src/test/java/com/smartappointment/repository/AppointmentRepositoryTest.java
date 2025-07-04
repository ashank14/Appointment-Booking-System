package com.smartappointment.repository;

import com.smartappointment.entity.Appointment;
import com.smartappointment.entity.Slot;
import com.smartappointment.entity.User;
import com.smartappointment.util.enumerations.AppointmentStatus;
import com.smartappointment.util.enumerations.SlotStatus;
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
class AppointmentRepositoryTest {

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
    private AppointmentRepository appointmentRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testFindByUser() {
        User user = userRepository.save(User.builder()
                .username("john")
                .email("john@example.com")
                .password("secret")
                .role("USER")
                .build());

        User provider = userRepository.save(User.builder()
                .username("doc")
                .email("doc@clinic.com")
                .password("pass")
                .role("PROVIDER")
                .build());

        Slot slot = slotRepository.save(Slot.builder()
                .provider(provider)
                .description("Consultation")
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .status(SlotStatus.AVAILABLE)
                .build());

        Appointment appointment = Appointment.builder()
                .user(user)
                .slot(slot)
                .status(AppointmentStatus.BOOKED)
                .build();

        appointmentRepository.save(appointment);

        List<Appointment> appointments = appointmentRepository.findByUser(user);
        assertEquals(1, appointments.size());
        assertEquals("Consultation", appointments.get(0).getSlot().getDescription());
    }
}
