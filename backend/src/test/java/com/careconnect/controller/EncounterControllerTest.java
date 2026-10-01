package com.careconnect.controller;

import com.careconnect.dto.*;
import com.careconnect.entity.*;
import com.careconnect.repository.*;
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
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EncounterControllerTest {

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
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        encounterRepository.deleteAll();
        appointmentRepository.deleteAll();
        doctorRepository.deleteAll();
        patientRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndLogin(String email, String password, Role role) throws Exception {
        var register = new RegisterRequest(email, password, role);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private Long createPatient(String token, String email, String firstName, String lastName) throws Exception {
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        PatientRequest request = new PatientRequest(userId, firstName, lastName, null, null, null, null, null, null);
        String json = mockMvc.perform(post("/api/patients")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private Long createDoctor(String token, Long userId, Long departmentId, String firstName, String lastName,
            String license) throws Exception {
        DoctorRequest request = new DoctorRequest(userId, departmentId, firstName, lastName, null, license, null);
        String json = mockMvc.perform(post("/api/doctors")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private Long createAppointment(String token, Long patientId, Long doctorId, LocalDate date, LocalTime time)
            throws Exception {
        AppointmentRequest request = new AppointmentRequest(patientId, doctorId, date, time, "Consultation",
                "Initial notes");
        String json = mockMvc.perform(post("/api/appointments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    @Test
    @DisplayName("Patient and doctor can create and retrieve valid encounters within appointment ownership")
    void createRetrieveAndUpdateEncounter() throws Exception {
        String adminToken = registerAndLogin("admin.m9@example.com", "Password123", Role.ADMIN);
        String patientToken = registerAndLogin("patient.m9@example.com", "Password123", Role.PATIENT);
        String doctorToken = registerAndLogin("doctor.m9@example.com", "Password123", Role.DOCTOR);

        Long patientUserId = userRepository.findByEmail("patient.m9@example.com").orElseThrow().getId();
        Long doctorUserId = userRepository.findByEmail("doctor.m9@example.com").orElseThrow().getId();

        Long patientId = createPatient(adminToken, "patient.m9@example.com", "Maya", "Patel");

        Long deptId = objectMapper.readTree(mockMvc.perform(post("/api/departments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepartmentRequest("Cardiology", null))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();

        Long doctorId = createDoctor(adminToken, doctorUserId, deptId, "Nina", "Shah", "LIC-ENCOUNTER-001");
        Long appointmentId = createAppointment(patientToken, patientId, doctorId, LocalDate.now().plusDays(3),
                LocalTime.of(10, 0));

        EncounterRequest request = new EncounterRequest(patientId, doctorId, appointmentId,
                LocalDate.now().plusDays(3), "Chest pain", "Mild asthma flare", "Albuterol treatment",
                "Rest and monitoring", "HR 88, BP 120/80");

        String createdJson = mockMvc.perform(post("/api/encounters")
                .header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(patientId))
                .andExpect(jsonPath("$.doctorId").value(doctorId))
                .andExpect(jsonPath("$.appointmentId").value(appointmentId))
                .andExpect(jsonPath("$.chiefComplaint").value("Chest pain"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long encounterId = objectMapper.readTree(createdJson).get("id").asLong();

        mockMvc.perform(get("/api/encounters/" + encounterId)
                .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(encounterId));

        EncounterRequest updateRequest = new EncounterRequest(patientId, doctorId, appointmentId,
                LocalDate.now().plusDays(3), "Chest pain", "Resolved asthma flare with monitoring",
                "Continue inhaler and follow-up", "Patient improved after treatment", "HR 82, BP 118/78");

        mockMvc.perform(put("/api/encounters/" + encounterId)
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Resolved asthma flare with monitoring"));
    }

    @Test
    @DisplayName("Patients cannot access another patient's encounter by ID or create for someone else's appointment")
    void patientOwnershipProtection() throws Exception {
        String adminToken = registerAndLogin("admin.m9b@example.com", "Password123", Role.ADMIN);
        String patient1Token = registerAndLogin("patient1.m9@example.com", "Password123", Role.PATIENT);
        String patient2Token = registerAndLogin("patient2.m9@example.com", "Password123", Role.PATIENT);
        String doctorToken = registerAndLogin("doctor.m9b@example.com", "Password123", Role.DOCTOR);

        Long patient1UserId = userRepository.findByEmail("patient1.m9@example.com").orElseThrow().getId();
        Long patient2UserId = userRepository.findByEmail("patient2.m9@example.com").orElseThrow().getId();
        Long doctorUserId = userRepository.findByEmail("doctor.m9b@example.com").orElseThrow().getId();

        Long patient1Id = createPatient(adminToken, "patient1.m9@example.com", "Asha", "Patel");
        Long patient2Id = createPatient(adminToken, "patient2.m9@example.com", "Rohit", "Singh");

        Long deptId = objectMapper.readTree(mockMvc.perform(post("/api/departments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepartmentRequest("Neurology", null))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();

        Long doctorId = createDoctor(adminToken, doctorUserId, deptId, "Farah", "Iqbal", "LIC-ENCOUNTER-002");
        Long appointmentId = createAppointment(patient1Token, patient1Id, doctorId, LocalDate.now().plusDays(4),
                LocalTime.of(14, 0));

        EncounterRequest validRequest = new EncounterRequest(patient1Id, doctorId, appointmentId,
                LocalDate.now().plusDays(4), "Migraine", "Resolved with rest", "Hydration and rest", "Improved",
                "Normal vitals");

        Long encounterId = objectMapper.readTree(mockMvc.perform(post("/api/encounters")
                .header("Authorization", "Bearer " + patient1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/encounters/" + encounterId)
                .header("Authorization", "Bearer " + patient2Token))
                .andExpect(status().isForbidden());

        EncounterRequest invalidPatientRequest = new EncounterRequest(patient2Id, doctorId, appointmentId,
                LocalDate.now().plusDays(5), "Another symptom", "Wrong patient", "No treatment", "Bad attempt",
                "Vitals");

        mockMvc.perform(post("/api/encounters")
                .header("Authorization", "Bearer " + patient2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPatientRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Doctors can only access their own encounters and admin can access all")
    void doctorAuthorizationAndAdminAuthorization() throws Exception {
        String adminToken = registerAndLogin("admin.m9c@example.com", "Password123", Role.ADMIN);
        String patientToken = registerAndLogin("patient.m9c@example.com", "Password123", Role.PATIENT);
        String doctor1Token = registerAndLogin("doctor1.m9@example.com", "Password123", Role.DOCTOR);
        String doctor2Token = registerAndLogin("doctor2.m9@example.com", "Password123", Role.DOCTOR);

        Long patientUserId = userRepository.findByEmail("patient.m9c@example.com").orElseThrow().getId();
        Long doctor1UserId = userRepository.findByEmail("doctor1.m9@example.com").orElseThrow().getId();
        Long doctor2UserId = userRepository.findByEmail("doctor2.m9@example.com").orElseThrow().getId();

        Long patientId = createPatient(adminToken, "patient.m9c@example.com", "Zain", "Khan");

        Long deptId = objectMapper.readTree(mockMvc.perform(post("/api/departments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepartmentRequest("General", null))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();

        Long doctor1Id = createDoctor(adminToken, doctor1UserId, deptId, "Dr", "One", "LIC-ENCOUNTER-003");
        Long doctor2Id = createDoctor(adminToken, doctor2UserId, deptId, "Dr", "Two", "LIC-ENCOUNTER-004");
        Long appointmentId = createAppointment(patientToken, patientId, doctor1Id, LocalDate.now().plusDays(5),
                LocalTime.of(9, 30));

        EncounterRequest request = new EncounterRequest(patientId, doctor1Id, appointmentId,
                LocalDate.now().plusDays(5), "Follow-up", "Improved", "Continue care", "Stable", "BP normal");

        Long encounterId = objectMapper.readTree(mockMvc.perform(post("/api/encounters")
                .header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/encounters/" + encounterId)
                .header("Authorization", "Bearer " + doctor2Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/encounters/" + encounterId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(encounterId));
    }

    @Test
    @DisplayName("Encounter creation validates relationships, nonexistence, and bad payloads")
    void invalidRelationshipsAndValidation() throws Exception {
        String adminToken = registerAndLogin("admin.m9d@example.com", "Password123", Role.ADMIN);
        String patientToken = registerAndLogin("patient.m9d@example.com", "Password123", Role.PATIENT);
        String doctorToken = registerAndLogin("doctor.m9d@example.com", "Password123", Role.DOCTOR);

        Long patientId = createPatient(adminToken, "patient.m9d@example.com", "Mina", "Patel");
        Long doctorUserId = userRepository.findByEmail("doctor.m9d@example.com").orElseThrow().getId();

        Long deptId = objectMapper.readTree(mockMvc.perform(post("/api/departments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepartmentRequest("Pediatrics", null))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();

        Long doctorId = createDoctor(adminToken, doctorUserId, deptId, "Aarav", "Verma", "LIC-ENCOUNTER-005");
        Long appointmentId = createAppointment(patientToken, patientId, doctorId, LocalDate.now().plusDays(6),
                LocalTime.of(11, 0));

        EncounterRequest mismatchedPatient = new EncounterRequest(patientId + 999, doctorId, appointmentId,
                LocalDate.now().plusDays(6), "Bad patient", "Incorrect", "No treatment", "Wrong patient", "Vitals");
        mockMvc.perform(post("/api/encounters")
                .header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mismatchedPatient)))
                .andExpect(status().isNotFound());

        EncounterRequest mismatchedDoctor = new EncounterRequest(patientId, doctorId + 999, appointmentId,
                LocalDate.now().plusDays(6), "Bad doctor", "Incorrect", "No treatment", "Wrong doctor", "Vitals");
        mockMvc.perform(post("/api/encounters")
                .header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mismatchedDoctor)))
                .andExpect(status().isNotFound());

        EncounterRequest mismatchedAppointment = new EncounterRequest(patientId, doctorId, appointmentId + 999,
                LocalDate.now().plusDays(6), "Bad appointment", "Incorrect", "No treatment", "Wrong appointment",
                "Vitals");
        mockMvc.perform(post("/api/encounters")
                .header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mismatchedAppointment)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/encounters")
                .header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/encounters")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EncounterRequest(null, doctorId, appointmentId,
                        LocalDate.now().plusDays(6), "", "", "", "", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unauthenticated and wrong-role access is denied")
    void unauthenticatedAndWrongRoleAccess() throws Exception {
        String adminToken = registerAndLogin("admin.m9e@example.com", "Password123", Role.ADMIN);
        String patientToken = registerAndLogin("patient.m9e@example.com", "Password123", Role.PATIENT);
        String doctorToken = registerAndLogin("doctor.m9e@example.com", "Password123", Role.DOCTOR);
        String otherDoctorToken = registerAndLogin("doctor2.m9e@example.com", "Password123", Role.DOCTOR);

        Long patientId = createPatient(adminToken, "patient.m9e@example.com", "Nisha", "Rao");
        Long doctorUserId = userRepository.findByEmail("doctor.m9e@example.com").orElseThrow().getId();
        Long otherDoctorUserId = userRepository.findByEmail("doctor2.m9e@example.com").orElseThrow().getId();
        Long deptId = objectMapper.readTree(mockMvc.perform(post("/api/departments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepartmentRequest("Endocrinology", null))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();
        Long doctorId = createDoctor(adminToken, doctorUserId, deptId, "Ishita", "Mohan", "LIC-ENCOUNTER-006");
        Long otherDoctorId = createDoctor(adminToken, otherDoctorUserId, deptId, "Rita", "Sharma", "LIC-ENCOUNTER-007");
        Long appointmentId = createAppointment(patientToken, patientId, doctorId, LocalDate.now().plusDays(7),
                LocalTime.of(15, 0));

        EncounterRequest request = new EncounterRequest(patientId, doctorId, appointmentId,
                LocalDate.now().plusDays(7), "Lab review", "Follow-up", "Review labs", "Stable", "Low risk");

        mockMvc.perform(post("/api/encounters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        Long encounterId = objectMapper.readTree(mockMvc.perform(post("/api/encounters")
                .header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/encounters/" + encounterId)
                .header("Authorization", "Bearer " + otherDoctorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/encounters/" + encounterId)
                .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk());
    }
}
