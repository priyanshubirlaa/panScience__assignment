package com.panScience.assignment.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.panScience.assignment.dto.AuthRequest;
import com.panScience.assignment.dto.AuthResponse;
import com.panScience.assignment.service.AuthService;
import com.panScience.assignment.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req, HttpServletRequest request) {
        String ip = extractClientIp(request);
        String key = ip + ":register";
        if (!rateLimitService.isAllowed(key)) {
            return ResponseEntity.status(429).body(Map.of("error", "Too many requests, try again later"));
        }
        try {
            String token = authService.register(req);
            // on success, reset counters for this ip/register to avoid penalizing subsequent successful attempts
            rateLimitService.reset(key);
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req, HttpServletRequest request) {
        String ip = extractClientIp(request);
        String key = ip + ":login";
        if (!rateLimitService.isAllowed(key)) {
            return ResponseEntity.status(429).body(Map.of("error", "Too many requests, try again later"));
        }
        try {
            String token = authService.login(req);
            // successful login - reset attempts for this ip/login
            rateLimitService.reset(key);
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // helper to get client IP, respects X-Forwarded-For
    private String extractClientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
