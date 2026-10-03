package com.careconnect.controller;

import com.careconnect.dto.LoginRequest;
import com.careconnect.dto.PatientRequest;
import com.careconnect.entity.Role;
import com.careconnect.repository.PatientRepository;
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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndLogin(String email, String password, Role role) throws Exception {
        var register = new com.careconnect.dto.RegisterRequest(email, password, role);
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest(email, password);
        MvcResult res = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk()).andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    @Test
    @DisplayName("Admin can create, list, view and update patient; patient ownership enforced")
    void adminCreatesAndUpdatesPatient() throws Exception {
        String adminToken = registerAndLogin("admin.p@example.com", "Password123", Role.ADMIN);
        String patientUserToken = registerAndLogin("patient.a@example.com", "Password123", Role.PATIENT);
        // Get patient user id
        Long patientUserId = userRepository.findByEmail("patient.a@example.com").get().getId();

        PatientRequest createReq = new PatientRequest(patientUserId, "Alice", "Anderson", LocalDate.of(1990,1,1), "F", "12345", "Addr", "Mom", "O+");

        // Create patient
        MvcResult createRes = mockMvc.perform(post("/api/patients").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.userId").value(patientUserId)).andReturn();

        JsonNode created = objectMapper.readTree(createRes.getResponse().getContentAsString());
        Long patientId = created.get("id").asLong();

        // Admin list
        mockMvc.perform(get("/api/patients").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());

        // Admin view
        mockMvc.perform(get("/api/patients/" + patientId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Alice"));

        // Patient views own profile
        mockMvc.perform(get("/api/patients/" + patientId).header("Authorization", "Bearer " + patientUserToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(patientUserId));

        // Patient updates permitted fields
        PatientRequest updateReq = new PatientRequest(patientUserId, "Alice2", "Anderson", LocalDate.of(1990,1,1), "F", "54321", "Addr", "Dad", "O+");
        mockMvc.perform(put("/api/patients/" + patientId).header("Authorization", "Bearer " + patientUserToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Alice2"));
    }

    @Test
    @DisplayName("Patient cannot access another patient's profile")
    void patientCannotAccessAnotherProfile() throws Exception {
        String adminToken = registerAndLogin("admin.p2@example.com", "Password123", Role.ADMIN);
        String patient1Token = registerAndLogin("patient.b@example.com", "Password123", Role.PATIENT);
        String patient2Token = registerAndLogin("patient.c@example.com", "Password123", Role.PATIENT);

        Long user1 = userRepository.findByEmail("patient.b@example.com").get().getId();
        Long user2 = userRepository.findByEmail("patient.c@example.com").get().getId();

        PatientRequest r1 = new PatientRequest(user1, "Bob", "B", null, null, null, null, null, null);
        JsonNode created = objectMapper.readTree(mockMvc.perform(post("/api/patients").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(r1)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        Long p1 = created.get("id").asLong();

        // Patient2 tries to access patient1
        mockMvc.perform(get("/api/patients/" + p1).header("Authorization", "Bearer " + patient2Token))
                .andExpect(status().isForbidden());

        // Patient2 tries to update patient1
        PatientRequest upd = new PatientRequest(user1, "Bob2", "B", null, null, null, null, null, null);
        mockMvc.perform(put("/api/patients/" + p1).header("Authorization", "Bearer " + patient2Token)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(upd)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Patient self-profile is patient-only, self-scoped, and returns 404 without a profile")
    void patientCanRetrieveOwnProfileOnly() throws Exception {
        String adminToken = registerAndLogin("admin.self@example.com", "Password123", Role.ADMIN);
        String patientToken = registerAndLogin("patient.self@example.com", "Password123", Role.PATIENT);
        String doctorToken = registerAndLogin("doctor.self@example.com", "Password123", Role.DOCTOR);
        Long patientUserId = userRepository.findByEmail("patient.self@example.com").orElseThrow().getId();

        PatientRequest request = new PatientRequest(
                patientUserId, "Mia", "Patient", null, null, null, null, null, null);
        mockMvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/patients/me")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(patientUserId))
                .andExpect(jsonPath("$.firstName").value("Mia"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());

        mockMvc.perform(get("/api/patients/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/patients/me")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/patients/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        String patientWithoutProfileToken = registerAndLogin(
                "patient.no-profile@example.com", "Password123", Role.PATIENT);
        mockMvc.perform(get("/api/patients/me")
                        .header("Authorization", "Bearer " + patientWithoutProfileToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Unauthenticated access returns 401 and wrong roles 403")
    void unauthenticatedAndWrongRoles() throws Exception {
        mockMvc.perform(get("/api/patients")).andExpect(status().isUnauthorized());

        String patientToken = registerAndLogin("patient.d@example.com", "Password123", Role.PATIENT);
        mockMvc.perform(get("/api/patients").header("Authorization", "Bearer " + patientToken)).andExpect(status().isForbidden());
    }
}
