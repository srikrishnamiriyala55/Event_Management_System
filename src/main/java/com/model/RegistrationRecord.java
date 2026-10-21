package com.model;

import java.time.LocalDateTime;

public record RegistrationRecord(
        int eventId,
        String eventTitle,
        String rollNo,
        String name,
        String email,
        String phoneNumber,
        String collegeName,
        String department,
        int year,
        String passId,
        String paymentStatus,
        LocalDateTime registeredAt) {
}
