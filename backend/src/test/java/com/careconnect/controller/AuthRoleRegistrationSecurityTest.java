package com.careconnect.controller;

import com.careconnect.dto.LoginRequest;
import com.careconnect.dto.RegisterRequest;
import com.careconnect.entity.Role;
import com.careconnect.entity.User;
import com.careconnect.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest(properties = "app.security.allow-privileged-registration=false")
@AutoConfigureMockMvc
class AuthRoleRegistrationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void onlyAdministratorsCanRegisterPrivilegedRoles() throws Exception {
        User admin = userRepository.save(new User(
                "registration.admin@example.com",
                passwordEncoder.encode("AdminPassword123"),
                Role.ADMIN,
                true
        ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "attacker@example.com", "Password123", Role.ADMIN))))
                .andExpect(status().isForbidden());
        assertThat(userRepository.findByEmail("attacker@example.com")).isEmpty();

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(admin.getEmail(), "AdminPassword123"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = response.get("token").asText();

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "doctor.created@example.com", "Password123", Role.DOCTOR))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("DOCTOR"));
    }

    @Test
    void roleTestEndpointsAreNotRegisteredOutsideTheTestProfile() throws Exception {
        mockMvc.perform(get("/api/test/admin").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
}
