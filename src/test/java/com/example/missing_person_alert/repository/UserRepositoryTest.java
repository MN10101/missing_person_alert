package com.example.missing_person_alert.repository;

import com.example.missing_person_alert.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Initialize a test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setRole("ROLE_POLICE");
        entityManager.persist(testUser);
        entityManager.flush();
    }

    @Test
    void findsUserByUsername() {
        // Act
        Optional<User> foundUser = userRepository.findByUsername("testuser");

        // Assert
        assertTrue(foundUser.isPresent(), "User should be found");
        assertEquals("testuser", foundUser.get().getUsername(), "Username should match");
        assertEquals("encodedPassword", foundUser.get().getPassword(), "Password should match");
        assertEquals("ROLE_POLICE", foundUser.get().getRole(), "Role should match");
    }

    @Test
    void returnsEmptyForUnknownUsername() {
        // Act
        Optional<User> foundUser = userRepository.findByUsername("nonexistent");

        // Assert
        assertFalse(foundUser.isPresent(), "User should not be found");
    }

    @Test
    void savesNewUser() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("newPassword");
        newUser.setRole("ROLE_POLICE");

        // Act
        User savedUser = userRepository.save(newUser);
        entityManager.flush();

        // Assert
        Optional<User> foundUser = userRepository.findByUsername("newuser");
        assertTrue(foundUser.isPresent(), "Saved user should be found");
        assertEquals("newuser", foundUser.get().getUsername(), "Username should match");
        assertEquals("newPassword", foundUser.get().getPassword(), "Password should match");
        assertEquals("ROLE_POLICE", foundUser.get().getRole(), "Role should match");
    }
}