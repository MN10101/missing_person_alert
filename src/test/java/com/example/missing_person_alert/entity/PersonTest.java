package com.example.missing_person_alert.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class PersonTest {

    @Test
    void testPersonFields() {
        Person person = new Person();

        Long id = 1L;
        String fullName = "Tobi Ta";
        String imagePath = "/uploads/1f1a2340-681f-4325-bab7-790bd807fa39_2.jpg";
        LocalDateTime publishedAt = LocalDateTime.now();
        Double latitude = 52.5194;
        Double longitude = 13.4067;
        LocalDateTime expiresAt = publishedAt.plusDays(7);

        person.setId(id);
        person.setFullName(fullName);
        person.setImagePath(imagePath);
        person.setPublishedAt(publishedAt);
        person.setLastSeenLatitude(latitude);
        person.setLastSeenLongitude(longitude);
        person.setExpiresAt(expiresAt);

        assertEquals(id, person.getId());
        assertEquals(fullName, person.getFullName());
        assertEquals(imagePath, person.getImagePath());
        assertEquals(publishedAt, person.getPublishedAt());
        assertEquals(latitude, person.getLastSeenLatitude());
        assertEquals(longitude, person.getLastSeenLongitude());
        assertEquals(expiresAt, person.getExpiresAt());
    }
}

