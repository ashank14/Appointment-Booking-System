package com.smartappointment.service;

import com.smartappointment.dto.SigninRequestDto;
import com.smartappointment.dto.SigninResponseDto;
import com.smartappointment.entity.User;
import com.smartappointment.repository.UserRepository;
import com.smartappointment.security.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    public SigninResponseDto signIn(SigninRequestDto request) {

        // authenticate username + password
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid username or password");
        }

        // fetch UserDetails (for JWT generation)
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        // fetch your User entity for extra info
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // generate JWT token
        String token = jwtTokenUtil.generateToken(userDetails);

        // return response
        return new SigninResponseDto("Signed in Successfully", token, user.getUsername(), user.getRole());
    }
}
