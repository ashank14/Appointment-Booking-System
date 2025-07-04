package com.smartappointment.notification.kafka.consumer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    @KafkaListener(topics = "appointment-notification", groupId = "notification-group")
    public void listen(String message) {
        logger.info("Received Notification: {}", message);
    }
}
