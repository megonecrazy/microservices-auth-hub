package com.notificationservice.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shared event contract — must match the auth-service producer's payload exactly.
 * In a production setup, this would live in a shared library/schema registry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {

    private Long userId;
    private String username;
    private String email;
    private String otpCode;
    private LocalDateTime timestamp;
}
