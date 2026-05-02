package com.panScience.assignment.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panScience.assignment.dto.AuthRequest;
import com.panScience.assignment.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {

    MockMvc mockMvc;

    @Mock
    AuthService authService;

    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        AuthController ctrl = new AuthController(authService, null);
        mockMvc = MockMvcBuilders.standaloneSetup(ctrl).build();
    }

    @Test
    void registerReturnsTokenOnSuccess() throws Exception {
        AuthRequest req = new AuthRequest();
        req.setUsername("sam");
        req.setPassword("pw");

        when(authService.register(req)).thenReturn("tkn-1");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req))).andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("tkn-1"));
    }

    @Test
    void loginReturns401OnError() throws Exception {
        AuthRequest req = new AuthRequest();
        req.setUsername("sam");
        req.setPassword("pw");

        when(authService.login(req)).thenThrow(new RuntimeException("bad"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req))).andExpect(status().isUnauthorized())
                 .andExpect(jsonPath("$.error").exists());
    }
}
