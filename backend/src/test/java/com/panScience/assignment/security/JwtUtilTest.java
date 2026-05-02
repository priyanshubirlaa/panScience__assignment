package com.panScience.assignment.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class JwtUtilTest {

    @Test
    void generateAndValidateToken() {
        JwtUtil util = new JwtUtil("01234567890123456789012345678901"); // 32 bytes key
        String token = util.generateToken("alice");
        assertNotNull(token);
        assertTrue(util.validateToken(token));
        assertEquals("alice", util.extractUsername(token));
    }

    @Test
    void invalidTokenReturnsFalse() {
        JwtUtil util = new JwtUtil("01234567890123456789012345678901");
        assertFalse(util.validateToken("not.a.token"));
    }
}
