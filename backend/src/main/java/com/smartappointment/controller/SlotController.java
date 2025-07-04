package com.smartappointment.controller;

import com.smartappointment.dto.SlotRequestDto;
import com.smartappointment.dto.SlotResponseDto;
import com.smartappointment.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/slots")
public class SlotController {

    @Autowired
    private SlotService slotService;

    //add slot
    @PreAuthorize("hasRole('PROVIDER')")
    @PostMapping("/addSlot")
    public ResponseEntity<SlotResponseDto> addSlot(@Valid @RequestBody SlotRequestDto requestDto,
                                                   Authentication authentication) {
        String username = authentication.getName();
        SlotResponseDto responseDto = slotService.addSlot(requestDto, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    //get slots of a provider
    @PreAuthorize("hasRole('PROVIDER')")
    @GetMapping("/getMySlots")
    public ResponseEntity<List<SlotResponseDto>> getMySlots(Authentication authentication) {
        log.info("getting my slots");
        String username = authentication.getName();
        System.out.println("Authenticated username: " + authentication.getName());

        List<SlotResponseDto> slots = slotService.getSlotsForProvider(username);
        return ResponseEntity.ok(slots);
    }



    //get all slots
    @GetMapping("/getAllSlots")
    public ResponseEntity<List<SlotResponseDto>> getAllSlots(){
        List<SlotResponseDto> slots=slotService.getAllSlots();
        return ResponseEntity.ok(slots);
    }

    //get slot by id
    @GetMapping("/{id}")
    public ResponseEntity<SlotResponseDto> getById(@PathVariable Long id){
        Optional<SlotResponseDto> slot=slotService.getById(id);
        return  slot
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
    }

    @PreAuthorize("hasRole('PROVIDER')")
    @DeleteMapping("/{id}")
    public String deleteSlot(@PathVariable Long id, Authentication authentication){
        String username=authentication.getName();
        return slotService.deleteSlot(id,username);
    }

    @PreAuthorize("hasRole('PROVIDER')")
    @PutMapping("/updateSlot/{slotId}")
    public ResponseEntity<SlotResponseDto> updateSlot(@PathVariable Long slotId, @Valid @RequestBody SlotRequestDto request, Authentication authentication){
        SlotResponseDto response=slotService.updateSlot(slotId,authentication.getName(),request);
        return ResponseEntity.ok(response);


    }
}
