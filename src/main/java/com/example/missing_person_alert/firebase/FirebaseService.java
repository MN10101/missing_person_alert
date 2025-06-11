package com.example.missing_person_alert.firebase;

import com.example.missing_person_alert.controller.NotificationController;
import com.example.missing_person_alert.entity.Person;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Locale;

@Service
public class FirebaseService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseService.class);

    @Value("${firebase.service-account-key-path}")
    private Resource serviceAccountResource;

    private final NotificationController notificationController;
    private final MessageSource messageSource;
    private boolean firebaseInitialized = false;

    public FirebaseService(NotificationController notificationController, MessageSource messageSource) {
        this.notificationController = notificationController;
        this.messageSource = messageSource;
    }

    @PostConstruct
    public void initialize() {
        try {
            if (!serviceAccountResource.exists()) {
                logger.error("Firebase service account file not found at: {}", serviceAccountResource.getFilename());
                return;
            }

            logger.info("Loading Firebase service account from: {}", serviceAccountResource.getFilename());
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountResource.getInputStream()))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                firebaseInitialized = true;
                logger.info("Firebase initialized successfully");
            }
        } catch (IOException e) {
            logger.error("Failed to initialize Firebase: {}", e.getMessage(), e);
            firebaseInitialized = false;
        }
    }

    @PreDestroy
    public void shutdown() {
        if (firebaseInitialized) {
            logger.info("Shutting down FirebaseApp");
            FirebaseApp.getApps().forEach(app -> {
                app.delete();
                logger.info("FirebaseApp {} deleted", app.getName());
            });
            firebaseInitialized = false;
        }
    }

    public void sendNotification(Person person, Locale locale) {
        if (!firebaseInitialized) {
            logger.warn("Firebase is not initialized. Skipping notification for: {}", person.getFullName());
            return;
        }

        String title = messageSource.getMessage("notification.title", null, locale);
        String body = messageSource.getMessage("notification.body", new Object[]{person.getFullName()}, locale);

        // Send to topic (mobile apps)
        Message topicMessage = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setTopic("Germany_Alerts")
                .build();

        try {
            String topicResponse = FirebaseMessaging.getInstance().send(topicMessage);
            logger.info("Successfully sent topic message: {}", topicResponse);
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send topic notification: {}", e.getMessage(), e);
        }

        // Send to registered web users
        for (String token : notificationController.getFcmTokens()) {
            Message webMessage = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setToken(token)
                    .build();

            try {
                String webResponse = FirebaseMessaging.getInstance().send(webMessage);
                logger.info("Successfully sent web notification to token {}: {}", token, webResponse);
            } catch (FirebaseMessagingException e) {
                logger.error("Failed to send web notification to token {}: {}", token, e.getMessage(), e);
            }
        }

        // Placeholder for Cell Broadcast integration
        logger.info("Placeholder: Send Cell Broadcast message for person: {}", person.getFullName());
    }
}