package com.example.missing_person_alert.controller;

import com.example.missing_person_alert.entity.Person;
import com.example.missing_person_alert.firebase.FirebaseService;
import com.example.missing_person_alert.model.EmergencyAlert;
import com.example.missing_person_alert.service.EmergencyAlertService;
import com.example.missing_person_alert.service.PersonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonControllerTest {

    @Mock
    private PersonService personService;

    @Mock
    private FirebaseService firebaseService;

    @Mock
    private EmergencyAlertService emergencyAlertService;

    @Mock
    private MessageSource messageSource;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Model model;

    @InjectMocks
    private PersonController personController;

    private Person testPerson;
    private EmergencyAlert testAlert;

    @BeforeEach
    void setUp() {
        testPerson = new Person();
        testPerson.setId(1L);
        testPerson.setFullName("Test Person");
        testPerson.setImagePath("test.jpg");
        testPerson.setPublishedAt(LocalDateTime.now());
        testPerson.setExpiresAt(LocalDateTime.now().plusDays(30));
        testPerson.setLastSeenLatitude(52.52);
        testPerson.setLastSeenLongitude(13.41);

        testAlert = new EmergencyAlert();
        testAlert.setHeadline("Test Alert");
        testAlert.setDescription("Test Description");
        testAlert.setAreaDesc("Test Area");
        testAlert.setSeverity("Severe");
    }

    @Test
    void displaysPublishPage() {
        when(emergencyAlertService.getCurrentAlerts()).thenReturn(Collections.singletonList(testAlert));

        String viewName = personController.showPublishPage(model);

        assertEquals("publish", viewName);
        verify(model).addAttribute("emergencyAlerts", Collections.singletonList(testAlert));
        verify(emergencyAlertService).getCurrentAlerts();
    }

    @Test
    void publishesAlert() throws IOException {
        MockMultipartFile image = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test image content".getBytes());

        when(personService.savePerson(anyString(), any(), any(), any()))
                .thenReturn(testPerson);

        ResponseEntity<Person> response = personController.publishAlert(
                "Test Person", image, 52.52, 13.41);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testPerson, response.getBody());
        verify(personService).savePerson(eq("Test Person"), any(), eq(52.52), eq(13.41));
        verify(firebaseService).sendNotification(testPerson, Locale.GERMAN);
    }

    @Test
    void handlesIOExceptionOnPublish() throws IOException {
        MockMultipartFile image = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test image content".getBytes());

        when(personService.savePerson(anyString(), any(), any(), any()))
                .thenThrow(new IOException("Test error"));

        ResponseEntity<Person> response = personController.publishAlert(
                "Test Person", image, 52.52, 13.41);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void showsAlertConfirmation() {
        when(personService.getPersonById(1L)).thenReturn(Optional.of(testPerson));

        String viewName = personController.showAlertConfirmation(1L, model);

        assertEquals("alert-confirmation", viewName);
        verify(model).addAttribute("person", testPerson);
        verify(model).addAttribute(eq("imageFilename"), anyString());
        verify(model).addAttribute(eq("locationName"), anyString());
    }

    @Test
    void fetchesAllPersons() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<Person> page = new PageImpl<>(Collections.singletonList(testPerson));

        when(personService.getAll(pageable)).thenReturn(page);

        ResponseEntity<List<Person>> response = personController.getAllPersons(0, 10, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(testPerson, response.getBody().get(0));
    }

    @Test
    void servesImage() throws IOException {
        // Setup
        String filename = "test.jpg";
        Path testFilePath = Paths.get("uploads", filename);
        Files.createDirectories(testFilePath.getParent());
        Files.write(testFilePath, "test image content".getBytes());

        try {
            // Test
            ResponseEntity<Resource> response = personController.serveImage(filename);

            // Verify
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
        } finally {
            // Clean up
            Files.deleteIfExists(testFilePath);
        }
    }

    @Test
    void returnsNotFoundForMissingImage() {
        ResponseEntity<Resource> response = personController.serveImage("nonexistent.jpg");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void handlesLocationNameInConfirmation() {
        when(personService.getPersonById(1L)).thenReturn(Optional.of(testPerson));

        String viewName = personController.showAlertConfirmation(1L, model);

        assertEquals("alert-confirmation", viewName);
        verify(model).addAttribute(eq("locationName"), anyString());
    }
}