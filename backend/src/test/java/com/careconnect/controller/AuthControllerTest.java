package com.careconnect.controller;

import com.careconnect.dto.LoginRequest;
import com.careconnect.dto.RegisterRequest;
import com.careconnect.entity.AuditAction;
import com.careconnect.entity.AuditLog;
import com.careconnect.entity.Role;
import com.careconnect.repository.AuditLogRepository;
import com.careconnect.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Registration: Should successfully register a new user with BCrypt password")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("patient@example.com", "Password123", Role.PATIENT);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("patient@example.com"))
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());

        // Verify password in DB is hashed with BCrypt and not stored in plaintext
        var userInDb = userRepository.findByEmail("patient@example.com").orElseThrow();
        assertThat(userInDb.getPassword()).startsWith("$2a$");
        assertThat(userInDb.getPassword()).isNotEqualTo("Password123");
    }

    @Test
    @DisplayName("2. Duplicate Email: Should reject registration when email already exists with 409 Conflict")
    void shouldRejectDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("duplicate@example.com", "Password123", Role.PATIENT);

        // First registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate registration attempt
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("An account with email 'duplicate@example.com' already exists"));
    }

    @Test
    @DisplayName("3. Successful Login: Should authenticate valid credentials and return JWT token and user info")
    void shouldLoginSuccessfully() throws Exception {
        // Register user
        RegisterRequest registerRequest = new RegisterRequest("login.success@example.com", "Password123", Role.PATIENT);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Login with correct credentials
        LoginRequest loginRequest = new LoginRequest("login.success@example.com", "Password123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.email").value("login.success@example.com"))
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        AuditLog auditLog = auditLogRepository.findByAction(AuditAction.LOGIN).stream()
                .filter(log -> log.getUserId().equals(userRepository.findByEmail("login.success@example.com")
                        .orElseThrow().getId()))
                .findFirst()
                .orElseThrow();
        assertThat(auditLog.getEntityType()).isEqualTo("User");
        assertThat(auditLog.getTimestamp()).isNotNull();
        assertThat(auditLog.getIpAddress()).isNotBlank();
        assertAuditContainsNoCredentials(auditLog, "Password123", token);
    }

    @Test
    @DisplayName("4. Invalid Password: Should return 401 Unauthorized when password does not match")
    void shouldRejectInvalidPassword() throws Exception {
        // Register user
        RegisterRequest registerRequest = new RegisterRequest("login.fail@example.com", "Password123", Role.PATIENT);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Login with wrong password
        LoginRequest loginRequest = new LoginRequest("login.fail@example.com", "WrongPassword999");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        Long userId = userRepository.findByEmail("login.fail@example.com").orElseThrow().getId();
        AuditLog auditLog = auditLogRepository.findByAction(AuditAction.FAILED_LOGIN).stream()
                .filter(log -> userId.equals(log.getUserId()))
                .findFirst()
                .orElseThrow();
        assertThat(auditLog.getTimestamp()).isNotNull();
        assertThat(auditLog.getIpAddress()).isNotBlank();
        assertAuditContainsNoCredentials(auditLog, "Password123", "WrongPassword999");
    }

    private void assertAuditContainsNoCredentials(AuditLog auditLog, String... credentials) {
        String serializedAuditFields = auditLog.getUserId() + "|" + auditLog.getAction() + "|"
                + auditLog.getEntityType() + "|" + auditLog.getEntityId() + "|"
                + auditLog.getTimestamp() + "|" + auditLog.getIpAddress();
        for (String credential : credentials) {
            assertThat(serializedAuditFields).doesNotContain(credential);
        }
    }

    @Test
    @DisplayName("5. Invalid JWT: Should return 401 Unauthorized when accessing protected endpoint with bad token")
    void shouldRejectInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer invalid.malformed.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("6. Protected Endpoint Without Token: Should return 401 Unauthorized when no Authorization header")
    void shouldRejectProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("7. Valid JWT Access: Should allow access to protected endpoint with valid JWT")
    void shouldAccessProtectedEndpointWithValidJwt() throws Exception {
        // Register user
        RegisterRequest registerRequest = new RegisterRequest("authenticated@example.com", "Password123", Role.DOCTOR);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Login to get token
        LoginRequest loginRequest = new LoginRequest("authenticated@example.com", "Password123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = responseJson.get("token").asText();

        // Access protected endpoint with valid token
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("authenticated@example.com"))
                .andExpect(jsonPath("$.role").value("DOCTOR"));
    }
}
