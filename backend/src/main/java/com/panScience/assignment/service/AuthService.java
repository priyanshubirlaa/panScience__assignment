package com.panScience.assignment.service;

import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.panScience.assignment.dto.AuthRequest;
import com.panScience.assignment.entity.User;
import com.panScience.assignment.repository.UserRepository;
import com.panScience.assignment.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String register(AuthRequest req) {
        Optional<User> existing = userRepository.findByUsername(req.getUsername());
        if (existing.isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User u = new User();
        u.setUsername(req.getUsername());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        userRepository.save(u);

        return jwtUtil.generateToken(u.getUsername());
    }

    public String login(AuthRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return jwtUtil.generateToken(user.getUsername());
    }
}
