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
class ClinicalNoteControllerTest {

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
    private ClinicalNoteRepository clinicalNoteRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        clinicalNoteRepository.deleteAll();
        encounterRepository.deleteAll();
        appointmentRepository.deleteAll();
        doctorRepository.deleteAll();
        patientRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndLogin(String email, String password, Role role) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, password, role))))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private Long createPatient(String token, String email, String firstName, String lastName) throws Exception {
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        PatientRequest request = new PatientRequest(userId, firstName, lastName, null, null, null, null, null, null);
        String response = mockMvc.perform(post("/api/patients")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createDoctor(String token, Long userId, Long departmentId, String firstName, String lastName,
                             String license) throws Exception {
        DoctorRequest request = new DoctorRequest(userId, departmentId, firstName, lastName, null, license, null);
        String response = mockMvc.perform(post("/api/doctors")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createAppointment(String token, Long patientId, Long doctorId, LocalDate date, LocalTime time)
            throws Exception {
        AppointmentRequest request = new AppointmentRequest(patientId, doctorId, date, time, "Consultation", "Initial notes");
        String response = mockMvc.perform(post("/api/appointments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createEncounter(String token, Long patientId, Long doctorId, Long appointmentId) throws Exception {
        EncounterRequest request = new EncounterRequest(
                patientId,
                doctorId,
                appointmentId,
                LocalDate.now().plusDays(2),
                "Persistent fatigue",
                "Likely viral illness",
                "Rest and hydration",
                "Improved with monitoring",
                "Normal vitals"
        );
        String response = mockMvc.perform(post("/api/encounters")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    @DisplayName("Doctor can create a clinical note for their own encounter")
    void doctorCanCreateClinicalNoteForOwnEncounter() throws Exception {
        String adminToken = registerAndLogin("admin.m10.1@example.com", "Password123", Role.ADMIN);
        String patientToken = registerAndLogin("patient.m10.1@example.com", "Password123", Role.PATIENT);
        String doctorToken = registerAndLogin("doctor.m10.1@example.com", "Password123", Role.DOCTOR);

        Long patientId = createPatient(adminToken, "patient.m10.1@example.com", "Ava", "Clark");
        Long doctorUserId = userRepository.findByEmail("doctor.m10.1@example.com").orElseThrow().getId();
        Long departmentId = objectMapper.readTree(mockMvc.perform(post("/api/departments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepartmentRequest("Cardiology", null))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();

        Long doctorId = createDoctor(adminToken, doctorUserId, departmentId, "Dr.", "Morris", "LIC-M10-001");
        Long appointmentId = createAppointment(patientToken, patientId, doctorId, LocalDate.now().plusDays(3), LocalTime.of(10, 0));
        Long encounterId = createEncounter(patientToken, patientId, doctorId, appointmentId);

        ClinicalNoteRequest request = new ClinicalNoteRequest(NoteType.PROGRESS, "Patient improved after rest and hydration.");

        mockMvc.perform(post("/api/encounters/{encounterId}/notes", encounterId)
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.encounterId").value(encounterId))
                .andExpect(jsonPath("$.doctorId").value(doctorId))
                .andExpect(jsonPath("$.noteType").value("PROGRESS"));
    }

    @Test
    @DisplayName("Authorized user can retrieve notes for an encounter")
    void authorizedUserCanRetrieveNotesForEncounter() throws Exception {
        String adminToken = registerAndLogin("admin.m10.2@example.com", "Password123", Role.ADMIN);
        String patientToken = registerAndLogin("patient.m10.2@example.com", "Password123", Role.PATIENT);
        String doctorToken = registerAndLogin("doctor.m10.2@example.com", "Password123", Role.DOCTOR);

        Long patientId = createPatient(adminToken, "patient.m10.2@example.com", "Liam", "Brown");
        Long doctorUserId = userRepository.findByEmail("doctor.m10.2@example.com").orElseThrow().getId();
        Long departmentId = objectMapper.readTree(mockMvc.perform(post("/api/departments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepartmentRequest("Neurology", null))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();

        Long doctorId = createDoctor(adminToken, doctorUserId, departmentId, "Dr.", "Khan", "LIC-M10-002");
        Long appointmentId = createAppointment(patientToken, patientId, doctorId, LocalDate.now().plusDays(4), LocalTime.of(11, 30));
        Long encounterId = createEncounter(patientToken, patientId, doctorId, appointmentId);

        mockMvc.perform(post("/api/encounters/{encounterId}/notes", encounterId)
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ClinicalNoteRequest(NoteType.GENERAL, "Follow-up review completed."))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/encounters/{encounterId}/notes", encounterId)
                .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].encounterId").value(encounterId));

        mockMvc.perform(get("/api/encounters/{encounterId}/notes", encounterId)
                .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Doctor can update their own clinical note")
    void doctorCanUpdateTheirOwnClinicalNote() throws Exception {
        String adminToken = registerAndLogin("admin.m10.3@example.com", "Password123", Role.ADMIN);
        String patientToken = registerAndLogin("patient.m10.3@example.com", "Password123", Role.PATIENT);
        String doctorToken = registerAndLogin("doctor.m10.3@example.com", "Password123", Role.DOCTOR);

        Long patientId = createPatient(adminToken, "patient.m10.3@example.com", "Noah", "Bennett");
        Long doctorUserId = userRepository.findByEmail("doctor.m10.3@example.com").orElseThrow().getId();
        Long departmentId = objectMapper.readTree(mockMvc.perform(post("/api/departments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepartmentRequest("Orthopedics", null))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();

        Long doctorId = createDoctor(adminToken, doctorUserId, departmentId, "Dr.", "Santos", "LIC-M10-003");
        Long appointmentId = createAppointment(patientToken, patientId, doctorId, LocalDate.now().plusDays(5), LocalTime.of(9, 15));
        Long encounterId = createEncounter(patientToken, patientId, doctorId, appointmentId);

        String createdResponse = mockMvc.perform(post("/api/encounters/{encounterId}/notes", encounterId)
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ClinicalNoteRequest(NoteType.ASSESSMENT, "Initial assessment recorded."))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long noteId = objectMapper.readTree(createdResponse).get("id").asLong();

        ClinicalNoteRequest updateRequest = new ClinicalNoteRequest(NoteType.TREATMENT_PLAN, "Continue monitoring and review labs in 48 hours.");

        mockMvc.perform(put("/api/notes/{id}", noteId)
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noteType").value("TREATMENT_PLAN"))
                .andExpect(jsonPath("$.content").value("Continue monitoring and review labs in 48 hours."));
    }

    @Test
    @DisplayName("Unauthorized doctor cannot modify another doctor's note")
    void unauthorizedDoctorCannotModifyAnotherDoctorsNote() throws Exception {
        String adminToken = registerAndLogin("admin.m10.4@example.com", "Password123", Role.ADMIN);
        String patientToken = registerAndLogin("patient.m10.4@example.com", "Password123", Role.PATIENT);
        String doctor1Token = registerAndLogin("doctor1.m10.4@example.com", "Password123", Role.DOCTOR);
        String doctor2Token = registerAndLogin("doctor2.m10.4@example.com", "Password123", Role.DOCTOR);

        Long patientId = createPatient(adminToken, "patient.m10.4@example.com", "Emma", "Davis");
        Long doctor1UserId = userRepository.findByEmail("doctor1.m10.4@example.com").orElseThrow().getId();
        Long doctor2UserId = userRepository.findByEmail("doctor2.m10.4@example.com").orElseThrow().getId();
        Long departmentId = objectMapper.readTree(mockMvc.perform(post("/api/departments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepartmentRequest("Family Medicine", null))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();

        Long doctor1Id = createDoctor(adminToken, doctor1UserId, departmentId, "Dr.", "Smith", "LIC-M10-004");
        Long doctor2Id = createDoctor(adminToken, doctor2UserId, departmentId, "Dr.", "Jones", "LIC-M10-005");
        Long appointmentId = createAppointment(patientToken, patientId, doctor1Id, LocalDate.now().plusDays(6), LocalTime.of(14, 0));
        Long encounterId = createEncounter(patientToken, patientId, doctor1Id, appointmentId);

        String created = mockMvc.perform(post("/api/encounters/{encounterId}/notes", encounterId)
                .header("Authorization", "Bearer " + doctor1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ClinicalNoteRequest(NoteType.PROGRESS, "Initial treatment progress."))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long noteId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(put("/api/notes/{id}", noteId)
                .header("Authorization", "Bearer " + doctor2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ClinicalNoteRequest(NoteType.GENERAL, "Unauthorized change."))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Patient cannot access another patient's notes")
    void patientCannotAccessAnotherPatientsNotes() throws Exception {
        String adminToken = registerAndLogin("admin.m10.5@example.com", "Password123", Role.ADMIN);
        String patient1Token = registerAndLogin("patient1.m10.5@example.com", "Password123", Role.PATIENT);
        String patient2Token = registerAndLogin("patient2.m10.5@example.com", "Password123", Role.PATIENT);
        String doctorToken = registerAndLogin("doctor.m10.5@example.com", "Password123", Role.DOCTOR);

        Long patient1Id = createPatient(adminToken, "patient1.m10.5@example.com", "Olivia", "Martinez");
        Long patient2Id = createPatient(adminToken, "patient2.m10.5@example.com", "Jacob", "Price");
        Long doctorUserId = userRepository.findByEmail("doctor.m10.5@example.com").orElseThrow().getId();
        Long departmentId = objectMapper.readTree(mockMvc.perform(post("/api/departments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepartmentRequest("Respiratory", null))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();

        Long doctorId = createDoctor(adminToken, doctorUserId, departmentId, "Dr.", "Rios", "LIC-M10-006");
        Long appointmentId = createAppointment(patient1Token, patient1Id, doctorId, LocalDate.now().plusDays(7), LocalTime.of(16, 0));
        Long encounterId = createEncounter(patient1Token, patient1Id, doctorId, appointmentId);

        mockMvc.perform(post("/api/encounters/{encounterId}/notes", encounterId)
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ClinicalNoteRequest(NoteType.GENERAL, "Sensitive note for patient 1."))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/encounters/{encounterId}/notes", encounterId)
                .header("Authorization", "Bearer " + patient2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Patient cannot create, update, or delete clinical notes")
    void patientCannotCreateUpdateOrDeleteClinicalNotes() throws Exception {
        String adminToken = registerAndLogin("admin.m10.6@example.com", "Password123", Role.ADMIN);
        String patientToken = registerAndLogin("patient.m10.6@example.com", "Password123", Role.PATIENT);
        String doctorToken = registerAndLogin("doctor.m10.6@example.com", "Password123", Role.DOCTOR);

        Long patientId = createPatient(adminToken, "patient.m10.6@example.com", "Mia", "Walker");
        Long doctorUserId = userRepository.findByEmail("doctor.m10.6@example.com").orElseThrow().getId();
        Long departmentId = objectMapper.readTree(mockMvc.perform(post("/api/departments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepartmentRequest("Dermatology", null))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();

        Long doctorId = createDoctor(adminToken, doctorUserId, departmentId, "Dr.", "Nguyen", "LIC-M10-007");
        Long appointmentId = createAppointment(patientToken, patientId, doctorId, LocalDate.now().plusDays(8), LocalTime.of(13, 0));
        Long encounterId = createEncounter(patientToken, patientId, doctorId, appointmentId);

        mockMvc.perform(post("/api/encounters/{encounterId}/notes", encounterId)
                .header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ClinicalNoteRequest(NoteType.GENERAL, "Forbidden create."))))
                .andExpect(status().isForbidden());

        String created = mockMvc.perform(post("/api/encounters/{encounterId}/notes", encounterId)
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ClinicalNoteRequest(NoteType.PROGRESS, "Allowed note for patient."))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long noteId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(put("/api/notes/{id}", noteId)
                .header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ClinicalNoteRequest(NoteType.GENERAL, "Forbidden update."))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/notes/{id}", noteId)
                .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Doctor cannot create a note for another doctor's encounter")
    void doctorCannotCreateNoteForAnotherDoctorsEncounter() throws Exception {
        String adminToken = registerAndLogin("admin.m10.7@example.com", "Password123", Role.ADMIN);
        String patientToken = registerAndLogin("patient.m10.7@example.com", "Password123", Role.PATIENT);
        String doctor1Token = registerAndLogin("doctor1.m10.7@example.com", "Password123", Role.DOCTOR);
        String doctor2Token = registerAndLogin("doctor2.m10.7@example.com", "Password123", Role.DOCTOR);

        Long patientId = createPatient(adminToken, "patient.m10.7@example.com", "Henry", "Parker");
        Long doctor1UserId = userRepository.findByEmail("doctor1.m10.7@example.com").orElseThrow().getId();
        Long doctor2UserId = userRepository.findByEmail("doctor2.m10.7@example.com").orElseThrow().getId();
        Long departmentId = objectMapper.readTree(mockMvc.perform(post("/api/departments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepartmentRequest("General Surgery", null))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();

        Long doctor1Id = createDoctor(adminToken, doctor1UserId, departmentId, "Dr.", "Allen", "LIC-M10-008");
        Long doctor2Id = createDoctor(adminToken, doctor2UserId, departmentId, "Dr.", "Brooks", "LIC-M10-009");
        Long appointmentId = createAppointment(patientToken, patientId, doctor1Id, LocalDate.now().plusDays(9), LocalTime.of(10, 45));
        Long encounterId = createEncounter(patientToken, patientId, doctor1Id, appointmentId);

        mockMvc.perform(post("/api/encounters/{encounterId}/notes", encounterId)
                .header("Authorization", "Bearer " + doctor2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ClinicalNoteRequest(NoteType.PROGRESS, "This should fail."))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Invalid or blank note content is rejected")
    void invalidOrBlankNoteContentIsRejected() throws Exception {
        String adminToken = registerAndLogin("admin.m10.8@example.com", "Password123", Role.ADMIN);
        String patientToken = registerAndLogin("patient.m10.8@example.com", "Password123", Role.PATIENT);
        String doctorToken = registerAndLogin("doctor.m10.8@example.com", "Password123", Role.DOCTOR);

        Long patientId = createPatient(adminToken, "patient.m10.8@example.com", "Sofia", "Howard");
        Long doctorUserId = userRepository.findByEmail("doctor.m10.8@example.com").orElseThrow().getId();
        Long departmentId = objectMapper.readTree(mockMvc.perform(post("/api/departments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepartmentRequest("Psychiatry", null))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();

        Long doctorId = createDoctor(adminToken, doctorUserId, departmentId, "Dr.", "Miller", "LIC-M10-010");
        Long appointmentId = createAppointment(patientToken, patientId, doctorId, LocalDate.now().plusDays(10), LocalTime.of(8, 30));
        Long encounterId = createEncounter(patientToken, patientId, doctorId, appointmentId);

        mockMvc.perform(post("/api/encounters/{encounterId}/notes", encounterId)
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ClinicalNoteRequest(NoteType.GENERAL, "   "))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/encounters/{encounterId}/notes", encounterId)
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"noteType\":\"GENERAL\",\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Missing or invalid note type is rejected")
    void missingOrInvalidNoteTypeIsRejected() throws Exception {
        String adminToken = registerAndLogin("admin.m10.9@example.com", "Password123", Role.ADMIN);
        String patientToken = registerAndLogin("patient.m10.9@example.com", "Password123", Role.PATIENT);
        String doctorToken = registerAndLogin("doctor.m10.9@example.com", "Password123", Role.DOCTOR);

        Long patientId = createPatient(adminToken, "patient.m10.9@example.com", "Grace", "Lee");
        Long doctorUserId = userRepository.findByEmail("doctor.m10.9@example.com").orElseThrow().getId();
        Long departmentId = objectMapper.readTree(mockMvc.perform(post("/api/departments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepartmentRequest("Pediatrics", null))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asLong();

        Long doctorId = createDoctor(adminToken, doctorUserId, departmentId, "Dr.", "North", "LIC-M10-011");
        Long appointmentId = createAppointment(patientToken, patientId, doctorId, LocalDate.now().plusDays(11), LocalTime.of(12, 30));
        Long encounterId = createEncounter(patientToken, patientId, doctorId, appointmentId);

        mockMvc.perform(post("/api/encounters/{encounterId}/notes", encounterId)
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Missing note type\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/encounters/{encounterId}/notes", encounterId)
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"noteType\":\"INVALID_TYPE\",\"content\":\"Test content\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Nonexistent encounter returns the appropriate error")
    void nonexistentEncounterReturnsNotFound() throws Exception {
        String adminToken = registerAndLogin("admin.m10.10@example.com", "Password123", Role.ADMIN);
        String doctorToken = registerAndLogin("doctor.m10.10@example.com", "Password123", Role.DOCTOR);

        mockMvc.perform(post("/api/encounters/999999/notes")
                .header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ClinicalNoteRequest(NoteType.GENERAL, "This encounter does not exist."))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/encounters/999999/notes")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
