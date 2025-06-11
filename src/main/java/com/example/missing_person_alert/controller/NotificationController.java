package com.example.missing_person_alert.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final List<String> fcmTokens = new ArrayList<>();

    @PostMapping("/register")
    public ResponseEntity<Void> registerToken(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        if (token != null && !fcmTokens.contains(token)) {
            fcmTokens.add(token);
        }
        return ResponseEntity.ok().build();
    }

    public List<String> getFcmTokens() {
        return fcmTokens;
    }
}