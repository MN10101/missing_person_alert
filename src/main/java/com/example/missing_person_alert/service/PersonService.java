package com.example.missing_person_alert.service;

import com.example.missing_person_alert.entity.Person;
import com.example.missing_person_alert.repository.PersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PersonService {

    private static final Logger logger = LoggerFactory.getLogger(PersonService.class);

    private final PersonRepository repository;

    @Value("${upload.dir}")
    private String uploadDir;

    public PersonService(PersonRepository repository) {
        this.repository = repository;
    }

    public Person savePerson(String name, MultipartFile image,
                             Double lastSeenLatitude, Double lastSeenLongitude) throws IOException {
        // Manual validation
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be empty");
        }

        String contentType = image.getContentType();
        if (!List.of("image/jpeg", "image/png").contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG and PNG images are allowed");
        }

        if (image.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Image size must not exceed 5MB");
        }

        String filename = UUID.randomUUID() + "_" + image.getOriginalFilename();
        Path path = Paths.get(uploadDir, filename);

        Files.createDirectories(path.getParent());
        Files.write(path, image.getBytes());

        Person person = new Person();
        person.setFullName(name.trim());
        person.setImagePath(filename);
        person.setPublishedAt(LocalDateTime.now());
        person.setLastSeenLatitude(lastSeenLatitude);
        person.setLastSeenLongitude(lastSeenLongitude);
        person.setExpiresAt(LocalDateTime.now().plusDays(30));
        Person saved = repository.save(person);
        logger.info("Saved person: id={}, name={}, publishedAt={}, expiresAt={}",
                saved.getId(), saved.getFullName(), saved.getPublishedAt(), saved.getExpiresAt());
        return saved;
    }

    public Page<Person> getAll(Pageable pageable) {
        List<Person> nonExpired = repository.findNonExpired();
        logger.info("Fetched {} non-expired persons from repository", nonExpired.size());
        nonExpired.forEach(person -> logger.debug("Person: id={}, name={}, expiresAt={}",
                person.getId(), person.getFullName(), person.getExpiresAt()));
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), nonExpired.size());
        List<Person> pagedList = nonExpired.subList(Math.min(start, nonExpired.size()), end);
        return new PageImpl<>(pagedList, pageable, nonExpired.size());
    }

    public Optional<Person> getPersonById(Long id) {
        return repository.findById(id);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void deleteExpiredRecords() {
        List<Person> expired = repository.findAll().stream()
                .filter(person -> person.getExpiresAt().isBefore(LocalDateTime.now()))
                .toList();
        repository.deleteAll(expired);
        logger.info("Deleted {} expired records", expired.size());
    }
}