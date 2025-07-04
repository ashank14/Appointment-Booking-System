package com.smartappointment.exception;

import com.smartappointment.dto.ErrorResponseDto;
import jakarta.persistence.EntityNotFoundException;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleResourceNotFound() {
        var ex = new ResourceNotFoundException("Resource missing");
        ResponseEntity<ErrorResponseDto> response = handler.handleResourceNotFound(ex);
        assertEquals(404, response.getStatusCodeValue());
        assertEquals("Resource missing", response.getBody().getMessage());
    }

    @Test
    void testHandleUsernameNotFound() {
        var ex = new UsernameNotFoundException("User not found");
        var response = handler.handleUsernameNotFound(ex);
        assertEquals(404, response.getStatusCodeValue());
        assertEquals("User not found", response.getBody().getMessage());
    }

    @Test
    void testHandleEntityNotFound() {
        var ex = new EntityNotFoundException("Entity missing");
        var response = handler.handleEntityNotFound(ex);
        assertEquals(404, response.getStatusCodeValue());
        assertEquals("Entity missing", response.getBody().getMessage());
    }

    @Test
    void testHandleResponseStatusException() {
        var ex = new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid request");
        var response = handler.handleResponseStatusException(ex);
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Invalid request", response.getBody().getMessage());
    }

    @Test
    void testHandleIllegalStateException() {
        var ex = new IllegalStateException("Slot already booked");
        var response = handler.handleIllegalStateException(ex);
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Slot already booked", response.getBody().getMessage());
    }

    @Test
    void testHandleMethodArgumentTypeMismatch() {
        var ex = new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, new IllegalArgumentException("Invalid"));
        var response = handler.handleMethodArgumentTypeMismatch(ex);
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().getMessage().contains("Invalid value 'abc' for parameter 'id'"));
    }

    @Test
    void testHandleAccessDeniedException() {
        var ex = new AccessDeniedException("Forbidden");
        var response = handler.handleAccessDeniedException(ex);
        assertEquals(403, response.getStatusCodeValue());
        assertTrue(response.getBody().getMessage().contains("Access denied"));
    }

    @Test
    void testHandleGenericException() {
        var ex = new RuntimeException("Unexpected issue");
        var response = handler.handleGenericException(ex);
        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().getMessage().contains("Unexpected error: Unexpected issue"));
    }
}
