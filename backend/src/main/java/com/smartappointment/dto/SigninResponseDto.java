package com.smartappointment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SigninResponseDto {
    private String message;
    private String token;
    private String username;
    private String role;
}
