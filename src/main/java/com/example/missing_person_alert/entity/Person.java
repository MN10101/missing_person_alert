package com.example.missing_person_alert.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "person")
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String imagePath;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    private Double lastSeenLatitude;
    private Double lastSeenLongitude;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // Getters and setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getImagePath() {
        return imagePath;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
    public Double getLastSeenLatitude() {
        return lastSeenLatitude;
    }
    public void setLastSeenLatitude(Double lastSeenLatitude) {
        this.lastSeenLatitude = lastSeenLatitude;
    }
    public Double getLastSeenLongitude() {
        return lastSeenLongitude;
    }
    public void setLastSeenLongitude(Double lastSeenLongitude) {
        this.lastSeenLongitude = lastSeenLongitude;
    }
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}