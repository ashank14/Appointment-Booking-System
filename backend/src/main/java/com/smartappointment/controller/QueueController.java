package com.smartappointment.controller;

import com.smartappointment.entity.User;
import com.smartappointment.repository.UserRepository;
import com.smartappointment.service.QueueService;
import com.smartappointment.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/queue")
public class QueueController {

    @Autowired
    private QueueService queueService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;

    @PostMapping("/join/{slotId}")
    public ResponseEntity<String> joinQueue(@PathVariable Long slotId, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        queueService.joinQueue(slotId, user);
        return ResponseEntity.ok("Added to queue");
    }

    @GetMapping("/{slotId}")
    public ResponseEntity<Integer> viewQueue(@PathVariable Long slotId) {
        return ResponseEntity.ok(queueService.getQueue(slotId).size());
    }

    @PostMapping("/leave/{slotId}")
    public ResponseEntity<String> leaveQueue(@PathVariable Long slotId, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        queueService.leaveQueue(slotId, user);
        return ResponseEntity.ok("Successfully left the queue.");
    }
}