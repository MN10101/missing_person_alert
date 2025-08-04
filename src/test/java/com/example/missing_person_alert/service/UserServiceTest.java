package com.example.missing_person_alert.service;

import com.example.missing_person_alert.entity.User;
import com.example.missing_person_alert.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private final String bcryptPassword = "$2a$10$7wzfKITgodTvH6wOx7F33eZyDHWRZTRje7MFw/aE8T2XAwdmIE0Ie";

    @BeforeEach
    void setUp() {
    }

    @Test
    void registersUser() {
        // Arrange
        String username = "leo";
        String password = "Pass123!";
        String role = "POLICE";

        when(userRepository.findByUsername(username.toLowerCase())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(bcryptPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        // Act
        userService.registerUser(username, password, role);

        // Assert
        verify(userRepository, times(1)).findByUsername(username.toLowerCase());
        verify(passwordEncoder, times(1)).encode(password);
        verify(userRepository, times(1)).save(argThat(user ->
                user.getUsername().equals(username.toLowerCase()) &&
                        user.getPassword().equals(bcryptPassword) &&
                        user.getRole().equals("ROLE_" + role.toUpperCase()) &&
                        user.getId() == 1L
        ));
    }

    @Test
    void rejectsDuplicateUsername() {
        // Arrange
        String username = "leo";
        String password = "Pass123!";
        String role = "POLICE";

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername(username.toLowerCase());
        existingUser.setPassword(bcryptPassword);
        existingUser.setRole("ROLE_POLICE");

        when(userRepository.findByUsername(username.toLowerCase())).thenReturn(Optional.of(existingUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(username, password, role),
                "Expected IllegalArgumentException for duplicate username"
        );
        assertEquals("Username already exists.", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(username.toLowerCase());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsInvalidPassword() {
        // Arrange
        String username = "leo";
        String invalidPassword = "pass";
        String role = "POLICE";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(username, invalidPassword, role),
                "Expected IllegalArgumentException for invalid password"
        );
        assertEquals("Password must include upper, lower, number, and special character.", exception.getMessage());
        // No repository interaction expected, as password validation fails first
        verify(userRepository, never()).findByUsername(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loadsUserByUsername() {
        // Arrange
        String username = "leo";
        User user = new User();
        user.setId(1L);
        user.setUsername(username.toLowerCase());
        user.setPassword(bcryptPassword);
        user.setRole("ROLE_POLICE");

        when(userRepository.findByUsername(username.toLowerCase())).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userService.loadUserByUsername(username);

        // Assert
        assertNotNull(userDetails, "UserDetails should not be null");
        assertEquals(username.toLowerCase(), userDetails.getUsername());
        assertEquals(bcryptPassword, userDetails.getPassword());
        assertEquals(1, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_POLICE")));
        verify(userRepository, times(1)).findByUsername(username.toLowerCase());
    }

    @Test
    void throwsExceptionForUnknownUsername() {
        // Arrange
        String username = "leo";
        when(userRepository.findByUsername(username.toLowerCase())).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userService.loadUserByUsername(username),
                "Expected UsernameNotFoundException for non-existent user"
        );
        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(username.toLowerCase());
    }
}