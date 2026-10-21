package com.model;

import java.time.LocalDateTime;

public record Event(
        int id,
        String title,
        String description,
        String category,
        String department,
        String venue,
        LocalDateTime date,
        LocalDateTime registrationClosesAt,
        int capacity,
        double fee,
        boolean hasImage,
        String organizerId,
        boolean active,
        int registeredCount) {
}
