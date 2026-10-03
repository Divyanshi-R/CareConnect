package com.careconnect.controller;

import com.careconnect.dto.AppointmentRequest;
import com.careconnect.dto.DepartmentRequest;
import com.careconnect.dto.DiagnosisRequest;
import com.careconnect.dto.DoctorRequest;
import com.careconnect.dto.EncounterRequest;
import com.careconnect.dto.LoginRequest;
import com.careconnect.dto.PatientRequest;
import com.careconnect.dto.RegisterRequest;
import com.careconnect.entity.DiagnosisStatus;
import com.careconnect.entity.Role;
import com.careconnect.repository.AppointmentRepository;
import com.careconnect.repository.DepartmentRepository;
import com.careconnect.repository.DiagnosisRepository;
import com.careconnect.repository.DoctorRepository;
import com.careconnect.repository.EncounterRepository;
import com.careconnect.repository.PatientRepository;
import com.careconnect.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DiagnosisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @AfterEach
    void cleanRepositories() {
        diagnosisRepository.deleteAll();
        encounterRepository.deleteAll();
        appointmentRepository.deleteAll();
        doctorRepository.deleteAll();
        patientRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    private String registerAndLogin(String email, Role role) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "Password123", role))))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Password123"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private Long createPatient(String adminToken, String email, String firstName) throws Exception {
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        PatientRequest request = new PatientRequest(userId, firstName, "Patient", null, null, null, null, null, null);
        String response = mockMvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createDoctor(String adminToken, String email, String license) throws Exception {
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        String departmentResponse = mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepartmentRequest("General Medicine " + license, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long departmentId = objectMapper.readTree(departmentResponse).get("id").asLong();

        DoctorRequest request = new DoctorRequest(userId, departmentId, "Test", "Doctor", null, license, null);
        String response = mockMvc.perform(post("/api/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createEncounter(String patientToken, Long patientId, Long doctorId, int dayOffset) throws Exception {
        String appointmentResponse = mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AppointmentRequest(
                                patientId, doctorId, LocalDate.now().plusDays(dayOffset),
                                LocalTime.of(10, 0), "Consultation", "Initial notes"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long appointmentId = objectMapper.readTree(appointmentResponse).get("id").asLong();

        EncounterRequest encounter = new EncounterRequest(
                patientId, doctorId, appointmentId, LocalDate.now().plusDays(dayOffset),
                "Routine review", "Initial assessment", "Observation", "Stable", "Normal");
        String encounterResponse = mockMvc.perform(post("/api/encounters")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(encounter)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(encounterResponse).get("id").asLong();
    }

    private DiagnosisRequest diagnosisRequest(Long patientId, Long doctorId, String name, DiagnosisStatus status) {
        return new DiagnosisRequest(
                patientId, doctorId, name, "Diagnosis description", "TEXT-001",
                LocalDateTime.now().withNano(0), status);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Test
    @DisplayName("Doctor creates and views own diagnosis; patient and admin can view authorized diagnoses")
    void createAndViewAuthorizedDiagnoses() throws Exception {
        String adminToken = registerAndLogin("admin.m11.1@example.com", Role.ADMIN);
        String patientToken = registerAndLogin("patient.m11.1@example.com", Role.PATIENT);
        String otherPatientToken = registerAndLogin("patient.m11.1b@example.com", Role.PATIENT);
        String doctorToken = registerAndLogin("doctor.m11.1@example.com", Role.DOCTOR);

        Long patientId = createPatient(adminToken, "patient.m11.1@example.com", "Ava");
        Long otherPatientId = createPatient(adminToken, "patient.m11.1b@example.com", "Mia");
        Long doctorId = createDoctor(adminToken, "doctor.m11.1@example.com", "LIC-M11-001");
        Long encounterId = createEncounter(patientToken, patientId, doctorId, 3);
        DiagnosisRequest request = diagnosisRequest(patientId, doctorId, "Acute bronchitis", DiagnosisStatus.ACTIVE);

        String created = mockMvc.perform(post("/api/encounters/{encounterId}/diagnoses", encounterId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.encounterId").value(encounterId))
                .andExpect(jsonPath("$.patientId").value(patientId))
                .andExpect(jsonPath("$.doctorId").value(doctorId))
                .andExpect(jsonPath("$.diagnosisName").value("Acute bronchitis"))
                .andExpect(jsonPath("$.diagnosisCode").value("TEXT-001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        Long diagnosisId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/encounters/{encounterId}/diagnoses", encounterId)
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(diagnosisId));

        mockMvc.perform(get("/api/patients/{patientId}/diagnoses", patientId)
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value(patientId));

        mockMvc.perform(get("/api/patients/{patientId}/diagnoses", patientId)
                        .header("Authorization", "Bearer " + otherPatientToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/patients/{patientId}/diagnoses", patientId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(diagnosisId));

        mockMvc.perform(get("/api/patients/{patientId}/diagnoses", otherPatientId)
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Patients cannot create or update diagnoses")
    void patientCannotCreateOrUpdateDiagnosis() throws Exception {
        String adminToken = registerAndLogin("admin.m11.2@example.com", Role.ADMIN);
        String patientToken = registerAndLogin("patient.m11.2@example.com", Role.PATIENT);
        String doctorToken = registerAndLogin("doctor.m11.2@example.com", Role.DOCTOR);
        Long patientId = createPatient(adminToken, "patient.m11.2@example.com", "Noah");
        Long doctorId = createDoctor(adminToken, "doctor.m11.2@example.com", "LIC-M11-002");
        Long encounterId = createEncounter(patientToken, patientId, doctorId, 4);
        DiagnosisRequest request = diagnosisRequest(patientId, doctorId, "Seasonal allergy", DiagnosisStatus.ACTIVE);

        mockMvc.perform(post("/api/encounters/{encounterId}/diagnoses", encounterId)
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isForbidden());

        String created = mockMvc.perform(post("/api/encounters/{encounterId}/diagnoses", encounterId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long diagnosisId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(put("/api/diagnoses/{id}", diagnosisId)
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(diagnosisRequest(patientId, doctorId, "Updated diagnosis", DiagnosisStatus.RESOLVED))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Doctors cannot create or update diagnoses for another doctor's encounter")
    void doctorCannotAccessAnotherDoctorsDiagnosis() throws Exception {
        String adminToken = registerAndLogin("admin.m11.3@example.com", Role.ADMIN);
        String patientToken = registerAndLogin("patient.m11.3@example.com", Role.PATIENT);
        String doctorToken = registerAndLogin("doctor.m11.3@example.com", Role.DOCTOR);
        String otherDoctorToken = registerAndLogin("doctor.m11.3b@example.com", Role.DOCTOR);
        Long patientId = createPatient(adminToken, "patient.m11.3@example.com", "Iris");
        Long doctorId = createDoctor(adminToken, "doctor.m11.3@example.com", "LIC-M11-003");
        Long otherDoctorId = createDoctor(adminToken, "doctor.m11.3b@example.com", "LIC-M11-004");
        Long encounterId = createEncounter(patientToken, patientId, doctorId, 5);
        DiagnosisRequest request = diagnosisRequest(patientId, doctorId, "Tension headache", DiagnosisStatus.ACTIVE);

        mockMvc.perform(post("/api/encounters/{encounterId}/diagnoses", encounterId)
                        .header("Authorization", "Bearer " + otherDoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isForbidden());

        String created = mockMvc.perform(post("/api/encounters/{encounterId}/diagnoses", encounterId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long diagnosisId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(put("/api/diagnoses/{id}", diagnosisId)
                        .header("Authorization", "Bearer " + otherDoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(diagnosisRequest(patientId, doctorId, "Updated headache", DiagnosisStatus.RESOLVED))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/encounters/{encounterId}/diagnoses", encounterId)
                        .header("Authorization", "Bearer " + otherDoctorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/patients/{patientId}/diagnoses", patientId)
                        .header("Authorization", "Bearer " + otherDoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/patients/{patientId}/diagnoses", patientId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].doctorId").value(doctorId));
    }

    @Test
    @DisplayName("Diagnosis endpoints validate resource IDs, relationships, and request fields")
    void invalidResourcesRelationshipsAndPayloadsAreRejected() throws Exception {
        String adminToken = registerAndLogin("admin.m11.4@example.com", Role.ADMIN);
        String patientToken = registerAndLogin("patient.m11.4@example.com", Role.PATIENT);
        String doctorToken = registerAndLogin("doctor.m11.4@example.com", Role.DOCTOR);
        Long patientId = createPatient(adminToken, "patient.m11.4@example.com", "Zoe");
        Long doctorId = createDoctor(adminToken, "doctor.m11.4@example.com", "LIC-M11-005");
        Long encounterId = createEncounter(patientToken, patientId, doctorId, 6);
        DiagnosisRequest validRequest = diagnosisRequest(patientId, doctorId, "Sinusitis", DiagnosisStatus.ACTIVE);

        mockMvc.perform(post("/api/encounters/{encounterId}/diagnoses", 999999L)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/patients/{patientId}/diagnoses", 999999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/encounters/{encounterId}/diagnoses", encounterId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(diagnosisRequest(patientId + 1, doctorId, "Wrong patient", DiagnosisStatus.ACTIVE))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/encounters/{encounterId}/diagnoses", encounterId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(diagnosisRequest(patientId, doctorId + 1, "Wrong doctor", DiagnosisStatus.ACTIVE))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/encounters/{encounterId}/diagnoses", encounterId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/encounters/{encounterId}/diagnoses", encounterId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":" + patientId + ",\"doctorId\":" + doctorId
                                + ",\"diagnosisName\":\" \",\"description\":\"Description\","
                                + "\"diagnosisCode\":\"CODE\",\"diagnosedAt\":\"2026-10-03T10:00:00\","
                                + "\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/encounters/{encounterId}/diagnoses", encounterId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":" + patientId + ",\"doctorId\":" + doctorId
                                + ",\"diagnosisName\":\"Valid\",\"description\":\"Description\","
                                + "\"diagnosisCode\":\"CODE\",\"diagnosedAt\":\"2026-10-03T10:00:00\","
                                + "\"status\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }
}
