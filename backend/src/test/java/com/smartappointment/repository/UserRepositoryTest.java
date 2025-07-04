package com.smartappointment.repository;

import com.smartappointment.entity.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

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
    private UserRepository userRepository;

    @Test
    void testSaveAndFindUser() {
        User user = User.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("password")
                .role("USER")
                .build();

        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getId());
        assertEquals("john_doe", savedUser.getUsername());
    }

    @Test
    void testFindByEmail() {
        User user = User.builder()
                .username("jane_doe")
                .email("jane@example.com")
                .password("password")
                .role("USER")
                .build();

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("jane@example.com");
        assertTrue(found.isPresent());
        assertEquals("jane_doe", found.get().getUsername());
    }

    @Test
    void testExistsByEmail() {
        User user = User.builder()
                .username("sam_smith")
                .email("sam@example.com")
                .password("password")
                .role("USER")
                .build();

        userRepository.save(user);

        assertTrue(userRepository.existsByEmail("sam@example.com"));
        assertFalse(userRepository.existsByEmail("notfound@example.com"));
    }

    @Test
    void testExistsByUsername() {
        User user = User.builder()
                .username("alice_wonder")
                .email("alice@example.com")
                .password("password")
                .role("USER")
                .build();

        userRepository.save(user);

        assertTrue(userRepository.existsByUsername("alice_wonder"));
        assertFalse(userRepository.existsByUsername("unknown_user"));
    }
}
