package com.example.missing_person_alert.controller;

import com.example.missing_person_alert.entity.Person;
import com.example.missing_person_alert.firebase.FirebaseService;
import com.example.missing_person_alert.model.EmergencyAlert;
import com.example.missing_person_alert.service.EmergencyAlertService;
import com.example.missing_person_alert.service.PersonService;
import com.example.missing_person_alert.util.DistanceCalculator;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonProperty;

@Controller
@RequestMapping
public class PersonController {

    private static final Logger logger = LoggerFactory.getLogger(PersonController.class);

    private final PersonService service;
    private final FirebaseService firebaseService;
    private final EmergencyAlertService emergencyAlertService;
    private final MessageSource messageSource;
    private final RestTemplate restTemplate;

    public PersonController(PersonService service, FirebaseService firebaseService,
                            EmergencyAlertService emergencyAlertService, MessageSource messageSource) {
        this.service = service;
        this.firebaseService = firebaseService;
        this.emergencyAlertService = emergencyAlertService;
        this.messageSource = messageSource;
        this.restTemplate = new RestTemplate();
    }

    private String getLocationName(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            logger.debug("No coordinates provided: lat={}, lon={}", latitude, longitude);
            return null;
        }
        if (latitude < 47.3 || latitude > 55.1 || longitude < 5.9 || longitude > 15.0) {
            logger.warn("Coordinates outside Germany: lat={}, lon={}", latitude, longitude);
            return "Coordinates outside Germany";
        }
        int retries = 2;
        long delay = 1000;
        for (int attempt = 1; attempt <= retries; attempt++) {
            try {
                String url = String.format("https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f&zoom=10&addressdetails=1", latitude, longitude);
                String userAgent = "MissingPersonAlertSystem/1.0 (contact: mamocool3@gmail.com)";
                restTemplate.getInterceptors().add((request, body, execution) -> {
                    request.getHeaders().set("User-Agent", userAgent);
                    return execution.execute(request, body);
                });
                NominatimResponse response = restTemplate.getForObject(url, NominatimResponse.class);
                logger.debug("Nominatim response for lat={}, lon={}: {}", latitude, longitude, response);
                if (response != null && response.getDisplayName() != null && !response.getDisplayName().isEmpty()) {
                    String displayName = response.getDisplayName();
                    logger.info("Parsed display_name: {}", displayName);
                    String[] parts = displayName.split(", ");
                    if (parts.length >= 2) {
                        String city = parts[0];
                        String country = parts[parts.length - 1];
                        String state = "";
                        for (int i = 1; i < parts.length - 1; i++) {
                            if (parts[i].matches(".*(Land|Bayern|Berlin|Hamburg).*")) {
                                state = parts[i];
                                break;
                            }
                        }
                        if (!state.isEmpty()) {
                            return String.format("%s, %s, %s", city, state, country);
                        }
                        return String.format("%s, %s", city, country);
                    }
                    return displayName;
                }
                logger.warn("No valid display_name in response for lat={}, lon={}", latitude, longitude);
                return String.format("Lat: %f, Lon: %f", latitude, longitude);
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt < retries) {
                    logger.warn("Rate limit hit, retrying after {}ms: lat={}, lon={}", delay, latitude, longitude);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    delay *= 2;
                } else {
                    logger.error("Max retries reached for lat={}, lon={}: {}", latitude, longitude, e.getMessage());
                    return String.format("Lat: %f, Lon: %f", latitude, longitude);
                }
            } catch (Exception e) {
                logger.error("Failed to fetch location name for lat={}, lon={}: {}", latitude, longitude, e.getMessage());
                return String.format("Lat: %f, Lon: %f", latitude, longitude);
            }
        }
        return String.format("Lat: %f, Lon: %f", latitude, longitude);
    }

    @GetMapping("/publish")
    public String showPublishPage(Model model) {
        List<EmergencyAlert> emergencyAlerts = emergencyAlertService.getCurrentAlerts();
        model.addAttribute("emergencyAlerts", emergencyAlerts);
        logger.info("Loaded publish page with {} emergency alerts", emergencyAlerts.size());
        return "publish";
    }

    @PostMapping("/api/persons/publish")
    @ResponseBody
    public ResponseEntity<Person> publishAlert(
            @RequestParam("name") @NotBlank String name,
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "lastSeenLatitude", required = false) Double lastSeenLatitude,
            @RequestParam(value = "lastSeenLongitude", required = false) Double lastSeenLongitude) {
        try {
            logger.debug("Publishing request: name={}, lat={}, lon={}", name, lastSeenLatitude, lastSeenLongitude);
            Person saved = service.savePerson(name, image, lastSeenLatitude, lastSeenLongitude);
            firebaseService.sendNotification(saved, Locale.GERMAN);
            logger.info("Successfully published alert for person id={}", saved.getId());
            return ResponseEntity.ok(saved);
        } catch (IOException e) {
            logger.error("Failed to publish alert: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid publish request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/alert-confirmation")
    public String showAlertConfirmation(@RequestParam("id") Long id, Model model) {
        Person person = service.getPersonById(id).orElseThrow(() -> new IllegalArgumentException("Person not found"));
        String filename = person.getImagePath().contains("/")
                ? person.getImagePath().substring(person.getImagePath().lastIndexOf("/") + 1)
                : person.getImagePath();
        String locationName = getLocationName(person.getLastSeenLatitude(), person.getLastSeenLongitude());
        logger.debug("Alert confirmation for id={}: locationName={}", id, locationName);
        model.addAttribute("person", person);
        model.addAttribute("imageFilename", filename);
        model.addAttribute("locationName", locationName);
        return "alert-confirmation";
    }

    @GetMapping("/api/persons")
    @ResponseBody
    public ResponseEntity<List<Person>> getAllPersons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(value = "userLatitude", required = false) Double userLatitude,
            @RequestParam(value = "userLongitude", required = false) Double userLongitude) {
        logger.info("Received request for /api/persons: page={}, size={}, userLatitude={}, userLongitude={}",
                page, size, userLatitude, userLongitude);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<Person> personPage = service.getAll(pageable);
        List<Person> persons = personPage.getContent();
        /*
        if (userLatitude != null && userLongitude != null) {
            final double MAX_DISTANCE_KM = 50.0;
            persons = persons.stream()
                    .filter(person -> person.getLastSeenLatitude() != null && person.getLastSeenLongitude() != null)
                    .filter(person -> {
                        double distance = DistanceCalculator.calculateDistance(
                                userLatitude, userLongitude,
                                person.getLastSeenLatitude(), person.getLastSeenLongitude()
                        );
                        return distance <= MAX_DISTANCE_KM;
                    })
                    .collect(Collectors.toList());
        }
        */

        logger.info("Returning {} persons for page {}, size={}", persons.size(), page, size);
        return ResponseEntity.ok(persons);
    }

    @GetMapping("/api/persons/image/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            Path file = Paths.get("uploads").resolve(filename);
            logger.debug("Serving image from path: {}", file);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(file);
                logger.info("Serving image: {} with content-type: {}", filename, contentType);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                        .body(resource);
            } else {
                logger.warn("Image not found or not readable: {}", filename);
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            logger.error("Error serving image {}: {}", filename, e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    private static class NominatimResponse {
        @JsonProperty("display_name")
        private String displayName;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return "NominatimResponse{displayName='" + displayName + "'}";
        }
    }
}