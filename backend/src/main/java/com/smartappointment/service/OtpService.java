package com.smartappointment.service;

import com.smartappointment.dto.RegisterRequestDto;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {
    private final Map<String, RegisterRequestDto> pendingUsers = new ConcurrentHashMap<>();
    private final Map<String, String> otpStore = new ConcurrentHashMap<>();

    public String generateAndStoreOtp(RegisterRequestDto request) {
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        otpStore.put(request.getUsername(), otp);
        pendingUsers.put(request.getUsername(), request);
        return otp;
    }

    public boolean verifyOtp(String username, String otp) {
        return otp.equals(otpStore.get(username));
    }

    public RegisterRequestDto getPendingUser(String username) {
        return pendingUsers.get(username);
    }

    public void clear(String username) {
        otpStore.remove(username);
        pendingUsers.remove(username);
    }

    public String getOtpForUser(String username) {
        return otpStore.get(username);
    }

}
