package com.example.missing_person_alert.controller;

import com.example.missing_person_alert.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class RegisterControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Model model;

    @InjectMocks
    private RegisterController registerController;

    @BeforeEach
    void setUp() {
    }

    @Test
    void displaysRegisterForm() {
        String viewName = registerController.showRegisterForm();

        assertEquals("register", viewName);
    }

    @Test
    void registersUserSuccessfully() {
        String username = "mamo";
        String password = "Pass123456789#";
        String role = "POLICE";

        String viewName = registerController.register(username, password, role, model);

        verify(userService).registerUser(username, password, role);
        verify(model).addAttribute("success", "Registration successful. Please log in.");
        assertEquals("login", viewName);
    }

    @Test
    void handlesRegistrationFailure() {
        String username = "mamo";
        String password = "Pass123456789#";
        String role = "POLICE";
        String errorMessage = "Username already exists";
        doThrow(new IllegalArgumentException(errorMessage))
                .when(userService).registerUser(anyString(), anyString(), anyString());

        String viewName = registerController.register(username, password, role, model);

        verify(userService).registerUser(username, password, role);
        verify(model).addAttribute("error", errorMessage);
        assertEquals("register", viewName);
    }
}