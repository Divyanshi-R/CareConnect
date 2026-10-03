package com.careconnect.controller;

import com.careconnect.dto.LoginRequest;
import com.careconnect.dto.MedicationRequest;
import com.careconnect.dto.RegisterRequest;
import com.careconnect.entity.MedicationForm;
import com.careconnect.entity.Role;
import com.careconnect.repository.MedicationRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MedicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        medicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndLogin(String email, Role role) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, "Password123", role))))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(email, "Password123"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private MedicationRequest medicationRequest(String name, String genericName, Boolean active) {
        return new MedicationRequest(
                name,
                genericName,
                MedicationForm.TABLET,
                "500 mg",
                "CareConnect Labs",
                "Medication description",
                active
        );
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Test
    @DisplayName("Admin can create, list, get, update, and delete medications")
    void adminCanManageMedicationsAndResponsesExposeExpectedFields() throws Exception {
        String adminToken = registerAndLogin("medication.admin@example.com", Role.ADMIN);

        MvcResult createResult = mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(medicationRequest("Paracetamol", "Acetaminophen", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Paracetamol"))
                .andExpect(jsonPath("$.genericName").value("Acetaminophen"))
                .andExpect(jsonPath("$.form").value("TABLET"))
                .andExpect(jsonPath("$.strength").value("500 mg"))
                .andExpect(jsonPath("$.manufacturer").value("CareConnect Labs"))
                .andExpect(jsonPath("$.description").value("Medication description"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long id = created.get("id").asLong();
        assertThat(created.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "id", "name", "genericName", "form", "strength", "manufacturer",
                "description", "active", "createdAt", "updatedAt");

        mockMvc.perform(get("/api/medications").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(get("/api/medications/{id}", id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(put("/api/medications/{id}", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(medicationRequest("Paracetamol 650", "Acetaminophen", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paracetamol 650"))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/medications/{id}", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Doctors can view medications; doctors and patients cannot manage them")
    void readAndWriteAuthorizationFollowsRoles() throws Exception {
        String adminToken = registerAndLogin("medication.auth.admin@example.com", Role.ADMIN);
        String doctorToken = registerAndLogin("medication.auth.doctor@example.com", Role.DOCTOR);
        String patientToken = registerAndLogin("medication.auth.patient@example.com", Role.PATIENT);

        String medicationJson = mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(medicationRequest("Ibuprofen", "Ibuprofen", true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(medicationJson).get("id").asLong();
        String requestJson = json(medicationRequest("Ibuprofen", "Ibuprofen", true));

        mockMvc.perform(get("/api/medications").header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(get("/api/medications/{id}", id).header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/medications").header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/medications/{id}", id).header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/medications/{id}", id).header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/medications").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/medications/{id}", id).header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/medications").header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/medications/{id}", id).header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/medications/{id}", id).header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/medications")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/medications")
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Medication endpoints reject missing IDs and invalid request fields")
    void missingMedicationAndInvalidRequestsAreRejected() throws Exception {
        String adminToken = registerAndLogin("medication.validation.admin@example.com", Role.ADMIN);

        mockMvc.perform(get("/api/medications/{id}", 999999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/medications/{id}", 999999L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(medicationRequest("Missing", "Missing", true))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/medications/{id}", 999999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","form":"TABLET","strength":"500 mg"}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Example","form":"INVALID","strength":"500 mg"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Medication search is case-insensitive across name and generic name")
    void medicationSearchMatchesNameAndGenericNameCaseInsensitively() throws Exception {
        String adminToken = registerAndLogin("medication.search.admin@example.com", Role.ADMIN);
        createMedication(adminToken, "Paracetamol", "Acetaminophen");
        createMedication(adminToken, "Pain Relief", "paracetamol");
        createMedication(adminToken, "Amoxicillin", "Amoxicillin");

        mockMvc.perform(get("/api/medications").param("search", "paracetamol")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/medications").param("search", "PARACETAMOL")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/medications").param("search", "acetaminophen")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Paracetamol"));
        mockMvc.perform(get("/api/medications").param("search", "does-not-exist")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/medications").param("search", "  ")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    private void createMedication(String token, String name, String genericName) throws Exception {
        mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(medicationRequest(name, genericName, true))))
                .andExpect(status().isCreated());
    }
}
