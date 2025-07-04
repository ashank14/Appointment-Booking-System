package com.smartappointment.notification.kafka.producer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.topic.name}")
    private String topic;

    @Value("${app.kafka.enabled}")
    private boolean kafkaEnabled;
    public void sendNotification(String message) {
        if (!kafkaEnabled) {
            logger.info("Kafka notification skipped: {}", message);
            return;
        }
        logger.info("Sending notification: {}", message);
        kafkaTemplate.send(topic, message);
    }
}
