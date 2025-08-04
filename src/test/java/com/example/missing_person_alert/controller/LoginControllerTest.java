package com.example.missing_person_alert.controller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginControllerTest {

    private final LoginController loginController = new LoginController();

    @Test
    void showLoginPage() {

        String viewName = loginController.showLoginPage();

        assertEquals("login", viewName);
    }
}
