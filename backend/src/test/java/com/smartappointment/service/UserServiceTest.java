package com.smartappointment.service;

import com.smartappointment.dto.RegisterRequestDto;
import com.smartappointment.dto.RegisterResponseDto;
import com.smartappointment.entity.User;
import com.smartappointment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    private RegisterRequestDto requestDto;
    private User savedUser;

    @BeforeEach
    void setup() {
        requestDto = RegisterRequestDto.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .role("PROVIDER")
                .specialization("Cardiology")
                .build();

        savedUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .role("PROVIDER")
                .specialization("Cardiology")
                .build();
    }

    @Test
    void testRegister_Success() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponseDto response = userService.register(requestDto);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("PROVIDER", response.getRole());
        assertEquals("Cardiology", response.getSpecialization());
    }

    @Test
    void testRegister_Conflict() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.register(requestDto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Username/Email already exists", ex.getReason());
    }

    @Test
    void testGetAllUsers() {
        User user1 = User.builder()
                .id(1L)
                .username("user1")
                .email("user1@example.com")
                .role("USER")
                .build();

        User user2 = User.builder()
                .id(2L)
                .username("provider1")
                .email("provider1@example.com")
                .role("PROVIDER")
                .specialization("Dentistry")
                .build();

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<RegisterResponseDto> users = userService.getAllUsers();

        assertEquals(2, users.size());
        assertEquals("user1", users.get(0).getUsername());
        assertEquals("Dentistry", users.get(1).getSpecialization());
    }

    @Test
    void testGetUserById_Found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(savedUser));

        Optional<RegisterResponseDto> result = userService.getUserById(1L);

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<RegisterResponseDto> result = userService.getUserById(1L);

        assertTrue(result.isEmpty());
    }
}
