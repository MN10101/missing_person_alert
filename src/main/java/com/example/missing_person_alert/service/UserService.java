package com.example.missing_person_alert.service;

import com.example.missing_person_alert.entity.User;
import com.example.missing_person_alert.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String PASSWORD_PATTERN =
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[!$%&/()=?+*.,;:@#-])[A-Za-z0-9!$%&/()=?+*.,;:@#-]{8,}$";

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().replace("ROLE_", ""))
                .build();
    }

    public void registerUser(String username, String password, String role) {
        if (!password.matches(PASSWORD_PATTERN)) {
            throw new IllegalArgumentException("Password must include upper, lower, number, and special character.");
        }

        if (userRepository.findByUsername(username.toLowerCase()).isPresent()) {
            throw new IllegalArgumentException("Username already exists.");
        }

        User user = new User();
        user.setUsername(username.toLowerCase());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ROLE_" + role.toUpperCase());
        userRepository.save(user);
    }
}