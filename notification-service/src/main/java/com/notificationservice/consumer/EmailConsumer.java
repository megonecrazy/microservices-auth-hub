package com.notificationservice.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.notificationservice.event.UserRegisteredEvent;
import com.notificationservice.service.EmailService;

@Component
public class EmailConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailConsumer.class);

    private final EmailService emailService;

    public EmailConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.user-registration:user-registration-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Consumed UserRegisteredEvent — user: {}, email: {}", event.getUsername(), event.getEmail());

        try {
            emailService.sendOtpEmail(event.getEmail(), event.getUsername(), event.getOtpCode());
            log.info("OTP email sent successfully to: {}", event.getEmail());
        } catch (Exception ex) {
            log.error("Failed to send OTP email to {}: {}", event.getEmail(), ex.getMessage(), ex);
            // The DefaultErrorHandler in KafkaConsumerConfig will retry 3 times
            throw ex;
        }
    }
}
