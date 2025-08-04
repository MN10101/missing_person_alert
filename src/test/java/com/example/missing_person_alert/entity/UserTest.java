package com.example.missing_person_alert.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void verifiesUserFromDatabase() {
        User user = new User();
        user.setId(1L);
        user.setUsername("leo");
        user.setPassword("$2a$10$7wzfKITgodTvH6wOx7F33eZyDHWRZTRje7MFw/aE8T2XAwdmIE0Ie");
        user.setRole("ROLE_POLICE");

        assertEquals(1L, user.getId());
        assertEquals("leo", user.getUsername());
        assertEquals("$2a$10$7wzfKITgodTvH6wOx7F33eZyDHWRZTRje7MFw/aE8T2XAwdmIE0Ie", user.getPassword());
        assertEquals("ROLE_POLICE", user.getRole());
    }

    @Test
    void checksPrefixedRole() {
        User user = new User();
        user.setRole("ROLE_USER");
        assertEquals("ROLE_USER", user.getRole());
    }
}