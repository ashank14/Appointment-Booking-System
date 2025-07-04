package com.smartappointment.controller;


import com.smartappointment.dto.RegisterRequestDto;
import com.smartappointment.dto.RegisterResponseDto;
import com.smartappointment.dto.SigninRequestDto;
import com.smartappointment.dto.SigninResponseDto;
import com.smartappointment.notification.NotificationService;
import com.smartappointment.notification.kafka.producer.KafkaProducerService;
import com.smartappointment.service.AuthService;
import com.smartappointment.service.OtpService;
import com.smartappointment.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;
    @Autowired
    OtpService otpService;

    @Autowired
    KafkaProducerService kafkaProducerService;
    @Autowired
    NotificationService notificationService;
    //register
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequestDto request){
        if (userService.existsByUsernameOrEmail(request.getUsername(), request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username/Email already exists");
        }

        String otp = otpService.generateAndStoreOtp(request);
        log.info("OTP for {} is {}", request.getUsername(), otp);
        kafkaProducerService.sendNotification("OTP for : "+request.getUsername()+" is : "+otp);
        notificationService.sendNotification(request.getEmail(), "OTP for : "+request.getUsername()+" is : "+otp);
        return ResponseEntity.ok("OTP sent. Please verify.");
    }

    @PostMapping("/verify")
    public ResponseEntity<RegisterResponseDto> verifyOtpAndCreateUser(@RequestParam String username, @RequestParam String otp) {
        if (!otpService.verifyOtp(username, otp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
        }

        RegisterRequestDto pendingUser = otpService.getPendingUser(username);
        if (pendingUser == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending registration");
        }

        RegisterResponseDto createdUser = userService.register(pendingUser);
        otpService.clear(username);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }


    @Autowired
    private AuthService authService;

    @PostMapping("/signin")
    public ResponseEntity<SigninResponseDto> signIn(@Valid @RequestBody SigninRequestDto request) {
        SigninResponseDto response = authService.signIn(request);
        return ResponseEntity.ok(response);
    }
    //get all users
    @GetMapping("/get-users")
    public ResponseEntity<List<RegisterResponseDto>> getAll(){
        List<RegisterResponseDto> response=userService.getAllUsers();
        return ResponseEntity.ok(response);
    }

    //get by id
    @GetMapping("/{id}")
    public ResponseEntity<RegisterResponseDto> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }




}
