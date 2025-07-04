package com.smartappointment.service;

import com.smartappointment.dto.SigninRequestDto;
import com.smartappointment.dto.SigninResponseDto;
import com.smartappointment.repository.UserRepository;
import com.smartappointment.security.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private UserRepository userRepository;

    private SigninRequestDto requestDto;

    @BeforeEach
    void setup() {
        requestDto = new SigninRequestDto("testuser", "password123");
    }

    @Test
    void testSignIn_Success() {
        Authentication mockAuth = mock(Authentication.class);
        UserDetails mockUserDetails = mock(UserDetails.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);

        when(userDetailsService.loadUserByUsername("testuser"))
                .thenReturn(mockUserDetails);

        when(jwtTokenUtil.generateToken(mockUserDetails))
                .thenReturn("mocked-jwt-token");

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(new com.smartappointment.entity.User())); // <-- missing stub

        SigninResponseDto response = authService.signIn(requestDto);

        assertNotNull(response);
        assertEquals("Signed in Successfully", response.getMessage());
        assertEquals("mocked-jwt-token", response.getToken());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService).loadUserByUsername("testuser");
        verify(jwtTokenUtil).generateToken(mockUserDetails);
    }
    @Test
    void testSignIn_InvalidCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.signIn(requestDto));

        assertEquals("Invalid username or password", ex.getMessage());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtTokenUtil, never()).generateToken(any());
    }
}
