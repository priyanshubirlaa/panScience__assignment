package com.panScience.assignment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.panScience.assignment.dto.AuthRequest;
import com.panScience.assignment.entity.User;
import com.panScience.assignment.repository.UserRepository;
import com.panScience.assignment.security.JwtUtil;

class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    JwtUtil jwtUtil;

    AuthService authService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository, jwtUtil);
    }

    @Test
    void registerCreatesUserAndReturnsToken() {
        AuthRequest req = new AuthRequest();
        req.setUsername("bob");
        req.setPassword("secret");

        when(userRepository.findByUsername("bob")).thenReturn(Optional.empty());
        when(jwtUtil.generateToken("bob")).thenReturn("token-123");

        String token = authService.register(req);
        assertEquals("token-123", token);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerWithExistingUserThrows() {
        AuthRequest req = new AuthRequest();
        req.setUsername("bob");
        req.setPassword("secret");

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(new User()));

        assertThrows(RuntimeException.class, () -> authService.register(req));
    }

    @Test
    void loginWithValidCredentialsReturnsToken() {
        AuthRequest req = new AuthRequest();
        req.setUsername("bob");
        req.setPassword("secret");

        User u = new User();
        u.setUsername("bob");
        // store bcrypt hash of 'secret' -> use same encoder
        u.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("secret"));

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(u));
        when(jwtUtil.generateToken("bob")).thenReturn("token-xyz");

        String token = authService.login(req);
        assertEquals("token-xyz", token);
    }

    @Test
    void loginWithInvalidPasswordThrows() {
        AuthRequest req = new AuthRequest();
        req.setUsername("bob");
        req.setPassword("wrong");

        User u = new User();
        u.setUsername("bob");
        u.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("secret"));

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(u));

        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    @Test
    void loginWithMissingUserThrows() {
        AuthRequest req = new AuthRequest();
        req.setUsername("nobody");
        req.setPassword("x");

        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login(req));
    }
}
