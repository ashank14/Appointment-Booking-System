package com.smartappointment.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartappointment.util.validation.ValidSpecialization;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidSpecialization
public class RegisterResponseDto {
    private Long id;
    private String username;
    private String email;
    private String role;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String specialization;

}
