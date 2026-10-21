package com.model;

import java.time.LocalDateTime;

public record ParticipantEventRegistration(
        Event event,
        String passId,
        String paymentStatus,
        LocalDateTime registeredAt) {
}
