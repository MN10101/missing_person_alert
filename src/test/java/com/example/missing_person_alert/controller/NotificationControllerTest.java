package com.example.missing_person_alert.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationControllerTest {

    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        notificationController = new NotificationController();
    }

    @Test
    void registersValidToken() {
        // Arrange
        Map<String, String> payload = new HashMap<>();
        String token = "test-token-123";
        payload.put("token", token);

        // Act
        ResponseEntity<Void> response = notificationController.registerToken(payload);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(notificationController.getFcmTokens().contains(token));
        assertEquals(1, notificationController.getFcmTokens().size());
    }

    @Test
    void ignoresDuplicateToken() {
        // Arrange
        Map<String, String> payload = new HashMap<>();
        String token = "test-token-123";
        payload.put("token", token);
        notificationController.registerToken(payload);

        // Act
        ResponseEntity<Void> response = notificationController.registerToken(payload);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, notificationController.getFcmTokens().size());
        assertTrue(notificationController.getFcmTokens().contains(token));
    }

    @Test
    void skipsNullToken() {
        // Arrange
        Map<String, String> payload = new HashMap<>();
        payload.put("token", null);

        // Act
        ResponseEntity<Void> response = notificationController.registerToken(payload);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(0, notificationController.getFcmTokens().size());
    }

    @Test
    void listsRegisteredTokens() {
        // Arrange
        Map<String, String> payload = new HashMap<>();
        String token = "test-token-123";
        payload.put("token", token);
        notificationController.registerToken(payload);

        // Act
        List<String> tokens = notificationController.getFcmTokens();

        // Assert
        assertEquals(1, tokens.size());
        assertTrue(tokens.contains(token));
    }
}