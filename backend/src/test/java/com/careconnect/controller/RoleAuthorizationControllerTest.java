package com.careconnect.controller;

import com.careconnect.dto.LoginRequest;
import com.careconnect.dto.RegisterRequest;
import com.careconnect.entity.Role;
import com.careconnect.repository.UserRepository;
import com.careconnect.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleAuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private String registerAndLogin(String email, String password, Role role) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(email, password, role);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginJson.get("token").asText();
    }

    @Test
    @DisplayName("Unauthenticated endpoints return 401")
    void unauthenticatedEndpointsReturn401() throws Exception {
        mockMvc.perform(get("/api/test/admin")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/test/doctor")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/test/patient")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Role based access and denials")
    void roleBasedAccessAndDenials() throws Exception {
        String adminToken = registerAndLogin("admin.role@example.com", "Password123", Role.ADMIN);
        String doctorToken = registerAndLogin("doctor.role@example.com", "Password123", Role.DOCTOR);
        String patientToken = registerAndLogin("patient.role@example.com", "Password123", Role.PATIENT);

        // Positive cases
        mockMvc.perform(get("/api/test/admin").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/test/doctor").header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/test/patient").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk());

        // Negative cases: wrong roles
        mockMvc.perform(get("/api/test/doctor").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/test/patient").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/test/admin").header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/test/patient").header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/test/admin").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/test/doctor").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Invalid and expired JWT handling")
    void invalidAndExpiredJwtHandling() throws Exception {
        // Invalid token
        mockMvc.perform(get("/api/test/admin").header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());

        // Expired token: generate one using jwtService helper with negative expiration
        String token = registerAndLogin("exp.user@example.com", "Password123", Role.ADMIN);
        var claims = jwtService.extractAllClaims(token);
        String expired = jwtService.generateTokenWithExpiration(claims, jwtService.extractEmail(token), -1000L);

        mockMvc.perform(get("/api/test/admin").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }
}
