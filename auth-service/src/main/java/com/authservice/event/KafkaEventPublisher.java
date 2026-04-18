package com.authservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@ConditionalOnProperty(name = "app.messaging.kafka.enabled", havingValue = "true")
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    @Value("${app.kafka.topics.user-registration:user-registration-events}")
    private String topic;

    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserRegistered(UserRegisteredEvent event) {
        CompletableFuture<SendResult<String, UserRegisteredEvent>> future =
                kafkaTemplate.send(topic, event.getEmail(), event);

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Failed to publish UserRegisteredEvent for {}: {}",
                        event.getEmail(), throwable.getMessage());
            } else {
                log.info("Published UserRegisteredEvent for {} to partition {} offset {}",
                        event.getEmail(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
