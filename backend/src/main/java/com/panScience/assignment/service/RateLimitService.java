package com.panScience.assignment.service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    // default settings: 1 minute window, 10 attempts
    private final long windowMs = Duration.ofMinutes(1).toMillis();
    private final int maxAttempts = 10;

    private final ConcurrentHashMap<String, ArrayDeque<Long>> attempts = new ConcurrentHashMap<>();

    public boolean isAllowed(String key) {
        long now = System.currentTimeMillis();
        ArrayDeque<Long> dq = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (dq) {
            // remove old timestamps
            while (!dq.isEmpty() && dq.peekFirst() <= now - windowMs) {
                dq.pollFirst();
            }

            if (dq.size() >= maxAttempts) {
                return false;
            }

            dq.addLast(now);
            return true;
        }
    }

    public void reset(String key) {
        attempts.remove(key);
    }
}
