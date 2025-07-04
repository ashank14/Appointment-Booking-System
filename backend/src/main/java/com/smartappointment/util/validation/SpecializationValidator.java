package com.smartappointment.util.validation;

import com.smartappointment.dto.RegisterRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SpecializationValidator implements ConstraintValidator<ValidSpecialization, RegisterRequestDto> {

    @Override
    public boolean isValid(RegisterRequestDto dto, ConstraintValidatorContext context) {
        if ("PROVIDER".equalsIgnoreCase(dto.getRole())) {
            return dto.getSpecialization() != null && !dto.getSpecialization().trim().isEmpty();
        }
        return true;
    }
}