package com.example.missing_person_alert.service;

import com.example.missing_person_alert.entity.Person;
import com.example.missing_person_alert.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private PersonService personService;

    private final String uploadDir = "test-uploads";

    @BeforeEach
    void setUp() {
        // Set the upload directory via reflection
        ReflectionTestUtils.setField(personService, "uploadDir", uploadDir);
    }

    @Test
    void savesPerson() throws IOException {
        // Arrange
        String name = "Tobi Ta";
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        Double latitude = 48.137154;
        Double longitude = 11.576124;

        Person savedPerson = new Person();
        savedPerson.setId(1L);
        savedPerson.setFullName(name);
        savedPerson.setImagePath("1f1a2340-681f-4325-bab7-790bd807fa39_test.jpg");
        savedPerson.setPublishedAt(LocalDateTime.now());
        savedPerson.setExpiresAt(LocalDateTime.now().plusDays(30));
        savedPerson.setLastSeenLatitude(latitude);
        savedPerson.setLastSeenLongitude(longitude);

        when(personRepository.save(any(Person.class))).thenReturn(savedPerson);

        // Act
        Person result = personService.savePerson(name, image, latitude, longitude);

        // Assert
        assertNotNull(result, "Saved person should not be null");
        assertEquals(name, result.getFullName());
        assertTrue(result.getImagePath().endsWith("test.jpg"));
        assertEquals(latitude, result.getLastSeenLatitude());
        assertEquals(longitude, result.getLastSeenLongitude());
        assertNotNull(result.getPublishedAt());
        assertNotNull(result.getExpiresAt());

        // Verify repository interaction
        verify(personRepository, times(1)).save(any(Person.class));

        // Clean up: Delete the created file and directory
        Path filePath = Paths.get(uploadDir, result.getImagePath());
        Files.deleteIfExists(filePath);
        Path dirPath = Paths.get(uploadDir);
        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            // Ensure directory is empty before deleting
            try (var stream = Files.list(dirPath)) {
                if (!stream.findAny().isPresent()) {
                    Files.deleteIfExists(dirPath);
                }
            }
        }
    }

    @Test
    void rejectsNullName() {
        // Arrange
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> personService.savePerson(null, image, null, null),
                "Expected IllegalArgumentException for null name"
        );
        assertEquals("Name cannot be blank", exception.getMessage());
        verify(personRepository, never()).save(any(Person.class));
    }

    @Test
    void rejectsEmptyImage() {
        // Arrange
        MockMultipartFile emptyImage = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                new byte[0]
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> personService.savePerson("Tobi Ta", emptyImage, null, null),
                "Expected IllegalArgumentException for empty image"
        );
        assertEquals("Image file cannot be empty", exception.getMessage());
        verify(personRepository, never()).save(any(Person.class));
    }

    @Test
    void rejectsInvalidImageType() {
        // Arrange
        MockMultipartFile invalidImage = new MockMultipartFile(
                "image",
                "test.txt",
                "text/plain",
                "invalid content".getBytes()
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> personService.savePerson("Tobi Ta", invalidImage, null, null),
                "Expected IllegalArgumentException for invalid image type"
        );
        assertEquals("Only JPEG and PNG images are allowed", exception.getMessage());
        verify(personRepository, never()).save(any(Person.class));
    }

    @Test
    void rejectsLargeImage() {
        // Arrange
        byte[] largeContent = new byte[6 * 1024 * 1024];
        MockMultipartFile largeImage = new MockMultipartFile(
                "image",
                "large.jpg",
                "image/jpeg",
                largeContent
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> personService.savePerson("Tobi Ta", largeImage, null, null),
                "Expected IllegalArgumentException for large image"
        );
        assertEquals("Image size must not exceed 5MB", exception.getMessage());
        verify(personRepository, never()).save(any(Person.class));
    }

    @Test
    void fetchesNonExpiredPersons() {
        // Arrange
        Person person1 = new Person();
        person1.setId(1L);
        person1.setFullName("Tobi Ta");
        person1.setImagePath("/uploads/1f1a2340-681f-4325-bab7-790bd807fa39_2.jpg");
        person1.setPublishedAt(LocalDateTime.now());
        person1.setExpiresAt(LocalDateTime.now().plusDays(1));

        Person person2 = new Person();
        person2.setId(2L);
        person2.setFullName("Tobi Ta");
        person2.setImagePath("/uploads/2f2a2340-681f-4325-bab7-790bd807fa39_3.jpg");
        person2.setPublishedAt(LocalDateTime.now());
        person2.setExpiresAt(LocalDateTime.now().plusDays(1));

        List<Person> nonExpired = Arrays.asList(person1, person2);
        when(personRepository.findNonExpired()).thenReturn(nonExpired);

        Pageable pageable = PageRequest.of(0, 1);

        // Act
        Page<Person> result = personService.getAll(pageable);

        // Assert
        assertNotNull(result, "Page should not be null");
        assertEquals(1, result.getContent().size(), "Should return 1 person per page");
        assertEquals(2, result.getTotalElements(), "Total elements should be 2");
        assertEquals(person1, result.getContent().get(0), "First person should match");
        verify(personRepository, times(1)).findNonExpired();
    }

    @Test
    void returnsEmptyPageForNoNonExpiredPersons() {
        // Arrange
        when(personRepository.findNonExpired()).thenReturn(Collections.emptyList());
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Person> result = personService.getAll(pageable);

        // Assert
        assertNotNull(result, "Page should not be null");
        assertTrue(result.getContent().isEmpty(), "Page content should be empty");
        assertEquals(0, result.getTotalElements(), "Total elements should be 0");
        verify(personRepository, times(1)).findNonExpired();
    }

    @Test
    void findsPersonById() {
        // Arrange
        Person person = new Person();
        person.setId(1L);
        person.setFullName("Tobi Ta");
        person.setImagePath("/uploads/1f1a2340-681f-4325-bab7-790bd807fa39_2.jpg");
        person.setPublishedAt(LocalDateTime.now());
        person.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(personRepository.findById(1L)).thenReturn(Optional.of(person));

        // Act
        Optional<Person> result = personService.getPersonById(1L);

        // Assert
        assertTrue(result.isPresent(), "Person should be found");
        assertEquals(person, result.get(), "Found person should match");
        verify(personRepository, times(1)).findById(1L);
    }

    @Test
    void returnsEmptyForUnknownPersonId() {
        // Arrange
        when(personRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        Optional<Person> result = personService.getPersonById(1L);

        // Assert
        assertFalse(result.isPresent(), "Person should not be found");
        verify(personRepository, times(1)).findById(1L);
    }

    @Test
    void deletesExpiredRecords() {
        // Arrange
        Person expiredPerson = new Person();
        expiredPerson.setId(1L);
        expiredPerson.setFullName("Tobi Ta");
        expiredPerson.setImagePath("/uploads/1f1a2340-681f-4325-bab7-790bd807fa39_2.jpg");
        expiredPerson.setPublishedAt(LocalDateTime.now().minusDays(31));
        expiredPerson.setExpiresAt(LocalDateTime.now().minusDays(1));

        Person nonExpiredPerson = new Person();
        nonExpiredPerson.setId(2L);
        nonExpiredPerson.setFullName("Tobi Ta");
        nonExpiredPerson.setImagePath("/uploads/2f2a2340-681f-4325-bab7-790bd807fa39_3.jpg");
        nonExpiredPerson.setPublishedAt(LocalDateTime.now());
        nonExpiredPerson.setExpiresAt(LocalDateTime.now().plusDays(1));

        List<Person> allPersons = Arrays.asList(expiredPerson, nonExpiredPerson);
        when(personRepository.findAll()).thenReturn(allPersons);
        doNothing().when(personRepository).deleteAll(anyIterable());

        // Act
        personService.deleteExpiredRecords();

        // Assert
        verify(personRepository, times(1)).findAll();
        verify(personRepository, times(1)).deleteAll(argThat(iterable -> {
            List<Person> deletedPersons = StreamSupport.stream(iterable.spliterator(), false)
                    .collect(Collectors.toList());
            return deletedPersons.size() == 1 && deletedPersons.contains(expiredPerson);
        }));
    }

    @Test
    void skipsDeletionForNoExpiredRecords() {
        // Arrange
        Person nonExpiredPerson = new Person();
        nonExpiredPerson.setId(1L);
        nonExpiredPerson.setFullName("Tobi Ta");
        nonExpiredPerson.setImagePath("/uploads/1f1a2340-681f-4325-bab7-790bd807fa39_2.jpg");
        nonExpiredPerson.setPublishedAt(LocalDateTime.now());
        nonExpiredPerson.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(personRepository.findAll()).thenReturn(Arrays.asList(nonExpiredPerson));
        doNothing().when(personRepository).deleteAll(anyIterable());

        // Act
        personService.deleteExpiredRecords();

        // Assert
        verify(personRepository, times(1)).findAll();
        verify(personRepository, times(1)).deleteAll(argThat(iterable -> {
            List<Person> deletedPersons = StreamSupport.stream(iterable.spliterator(), false)
                    .collect(Collectors.toList());
            return deletedPersons.isEmpty();
        }));
    }
}