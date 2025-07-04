package com.smartappointment.service;


import com.smartappointment.dto.RegisterRequestDto;
import com.smartappointment.dto.RegisterResponseDto;
import com.smartappointment.entity.User;
import com.smartappointment.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;


@Slf4j
@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    public boolean existsByUsernameOrEmail(String username, String email) {
        return userRepository.existsByUsername(username) || userRepository.existsByEmail(email);
    }


    public RegisterResponseDto register(RegisterRequestDto request){
        //check for existing user
        if(userRepository.existsByUsername(request.getUsername()) || userRepository.existsByEmail(request.getEmail())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username/Email already exists");
        }
        //create new user
        User newUser=User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .role(request.getRole())
                .specialization(("PROVIDER".equalsIgnoreCase(request.getRole()) ? request.getSpecialization() : null))
                .build();
        //save to db
        User savedUser=userRepository.save(newUser);
        //create and return response
        return RegisterResponseDto.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .role(savedUser.getRole())
                .specialization(savedUser.getSpecialization())
                .build();
    }

    public List<RegisterResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> RegisterResponseDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .specialization(user.getSpecialization())
                        .build())
                .toList();
    }
    public Optional<RegisterResponseDto> getUserById(Long id) {
        return userRepository.findById(id)
                .map(user -> RegisterResponseDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .specialization(user.getSpecialization())
                        .build());
    }



}
