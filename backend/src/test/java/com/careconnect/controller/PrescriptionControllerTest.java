package com.careconnect.controller;

import com.careconnect.dto.AppointmentRequest;
import com.careconnect.dto.DepartmentRequest;
import com.careconnect.dto.DoctorRequest;
import com.careconnect.dto.EncounterRequest;
import com.careconnect.dto.LoginRequest;
import com.careconnect.dto.PatientRequest;
import com.careconnect.dto.PrescriptionItemRequest;
import com.careconnect.dto.PrescriptionRequest;
import com.careconnect.dto.RegisterRequest;
import com.careconnect.entity.Medication;
import com.careconnect.entity.MedicationForm;
import com.careconnect.entity.PrescriptionStatus;
import com.careconnect.entity.Role;
import com.careconnect.repository.AppointmentRepository;
import com.careconnect.repository.ClinicalNoteRepository;
import com.careconnect.repository.DepartmentRepository;
import com.careconnect.repository.DiagnosisRepository;
import com.careconnect.repository.DoctorRepository;
import com.careconnect.repository.EncounterRepository;
import com.careconnect.repository.MedicationRepository;
import com.careconnect.repository.PatientRepository;
import com.careconnect.repository.PrescriptionItemRepository;
import com.careconnect.repository.PrescriptionRepository;
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
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PrescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private ClinicalNoteRepository clinicalNoteRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        prescriptionItemRepository.deleteAll();
        prescriptionRepository.deleteAll();
        diagnosisRepository.deleteAll();
        clinicalNoteRepository.deleteAll();
        encounterRepository.deleteAll();
        appointmentRepository.deleteAll();
        doctorRepository.deleteAll();
        patientRepository.deleteAll();
        medicationRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    private String registerAndLogin(String email, Role role) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(email, "Password123", role))))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, "Password123"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private TestActor createActor(String adminToken, String suffix, int dayOffset) throws Exception {
        String patientEmail = suffix + ".patient@example.com";
        String doctorEmail = suffix + ".doctor@example.com";
        String patientToken = registerAndLogin(patientEmail, Role.PATIENT);
        String doctorToken = registerAndLogin(doctorEmail, Role.DOCTOR);
        Long patientUserId = userRepository.findByEmail(patientEmail).orElseThrow().getId();
        Long doctorUserId = userRepository.findByEmail(doctorEmail).orElseThrow().getId();

        String patientResponse = mockMvc.perform(post("/api/patients")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PatientRequest(
                                patientUserId, "Test", "Patient", null, null, null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long patientId = objectMapper.readTree(patientResponse).get("id").asLong();

        String departmentResponse = mockMvc.perform(post("/api/departments")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new DepartmentRequest("M13 " + suffix, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long departmentId = objectMapper.readTree(departmentResponse).get("id").asLong();

        String doctorResponse = mockMvc.perform(post("/api/doctors")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new DoctorRequest(
                                doctorUserId, departmentId, "Test", "Doctor", null,
                                "LIC-" + suffix, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long doctorId = objectMapper.readTree(doctorResponse).get("id").asLong();

        LocalDate appointmentDate = LocalDate.now().plusDays(dayOffset);
        String appointmentResponse = mockMvc.perform(post("/api/appointments")
                        .header("Authorization", bearer(patientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AppointmentRequest(
                                patientId, doctorId, appointmentDate, LocalTime.of(10, 0),
                                "Consultation", "Initial notes"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long appointmentId = objectMapper.readTree(appointmentResponse).get("id").asLong();

        String encounterResponse = mockMvc.perform(post("/api/encounters")
                        .header("Authorization", bearer(patientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new EncounterRequest(
                                patientId, doctorId, appointmentId, appointmentDate,
                                "Consultation", "Assessment", "Treatment", "Notes", "Vitals"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long encounterId = objectMapper.readTree(encounterResponse).get("id").asLong();

        return new TestActor(patientToken, doctorToken, patientId, doctorId, encounterId);
    }

    private Medication createMedication(String name) {
        Medication medication = new Medication();
        medication.setName(name);
        medication.setGenericName(name);
        medication.setForm(MedicationForm.TABLET);
        medication.setStrength("500 mg");
        medication.setActive(true);
        return medicationRepository.save(medication);
    }

    private PrescriptionRequest prescriptionRequest(
            TestActor actor,
            Long patientId,
            Long doctorId,
            Long encounterId,
            List<Long> medicationIds,
            PrescriptionStatus status
    ) {
        List<PrescriptionItemRequest> items = medicationIds.stream()
                .map(medicationId -> new PrescriptionItemRequest(
                        medicationId, "1 tablet", "Twice daily", "5 days", "Oral", "After meals"))
                .toList();
        return new PrescriptionRequest(
                patientId, doctorId, encounterId, LocalDate.now(),
                "Follow the dosage instructions", status, items);
    }

    private PrescriptionRequest prescriptionRequest(TestActor actor, List<Long> medicationIds) {
        return prescriptionRequest(
                actor, actor.patientId(), actor.doctorId(), actor.encounterId(),
                medicationIds, PrescriptionStatus.ACTIVE);
    }

    private Long createPrescription(TestActor actor, List<Long> medicationIds) throws Exception {
        String response = mockMvc.perform(post("/api/prescriptions")
                        .header("Authorization", bearer(actor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(prescriptionRequest(actor, medicationIds))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Test
    @DisplayName("Doctor can create and retrieve own prescriptions with all items")
    void doctorCreatesPrescriptionWithMultipleItemsAndResponseExposesOnlyDtos() throws Exception {
        String adminToken = registerAndLogin("m13.admin@example.com", Role.ADMIN);
        TestActor actor = createActor(adminToken, "m13.one", 4);
        Medication firstMedication = createMedication("Medication One");
        Medication secondMedication = createMedication("Medication Two");

        MvcResult createResult = mockMvc.perform(post("/api/prescriptions")
                        .header("Authorization", bearer(actor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(prescriptionRequest(
                                actor, List.of(firstMedication.getId(), secondMedication.getId())))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(actor.patientId()))
                .andExpect(jsonPath("$.doctorId").value(actor.doctorId()))
                .andExpect(jsonPath("$.encounterId").value(actor.encounterId()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].prescriptionId").isNumber())
                .andExpect(jsonPath("$.items[0].medicationId").isNumber())
                .andExpect(jsonPath("$.items[0].dosage").value("1 tablet"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long prescriptionId = created.get("id").asLong();
        assertThat(created.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "id", "patientId", "doctorId", "encounterId", "prescribedDate",
                "instructions", "status", "createdAt", "updatedAt", "items");
        assertThat(created.toString()).doesNotContain("password", "jwt", "credentials");
        assertThat(created.get("items").get(0).get("prescriptionId").asLong()).isEqualTo(prescriptionId);
        assertThat(created.get("items").get(1).get("prescriptionId").asLong()).isEqualTo(prescriptionId);

        mockMvc.perform(get("/api/prescriptions/{id}", prescriptionId)
                        .header("Authorization", bearer(actor.doctorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));
        mockMvc.perform(get("/api/patients/{patientId}/prescriptions", actor.patientId())
                        .header("Authorization", bearer(actor.patientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(prescriptionId));
        mockMvc.perform(get("/api/doctors/{doctorId}/prescriptions", actor.doctorId())
                        .header("Authorization", bearer(actor.doctorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(prescriptionId));
    }

    @Test
    @DisplayName("Creation rejects other doctors, patients, anonymous users, and mismatched relationships")
    void creationAuthorizationAndRelationshipsAreEnforced() throws Exception {
        String adminToken = registerAndLogin("m13.create.admin@example.com", Role.ADMIN);
        TestActor firstActor = createActor(adminToken, "m13.create.one", 4);
        TestActor secondActor = createActor(adminToken, "m13.create.two", 5);
        Medication medication = createMedication("Creation Test Medication");
        String firstRequest = json(prescriptionRequest(firstActor, List.of(medication.getId())));

        mockMvc.perform(post("/api/prescriptions")
                        .header("Authorization", bearer(firstActor.patientToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequest))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequest))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/prescriptions")
                        .header("Authorization", bearer(firstActor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(prescriptionRequest(secondActor, List.of(medication.getId())))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/prescriptions")
                        .header("Authorization", bearer(firstActor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(prescriptionRequest(
                                firstActor, secondActor.patientId(), firstActor.doctorId(),
                                firstActor.encounterId(), List.of(medication.getId()), PrescriptionStatus.ACTIVE))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/prescriptions")
                        .header("Authorization", bearer(firstActor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(prescriptionRequest(
                                firstActor, firstActor.patientId(), secondActor.doctorId(),
                                firstActor.encounterId(), List.of(medication.getId()), PrescriptionStatus.ACTIVE))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/prescriptions")
                        .header("Authorization", bearer(firstActor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(prescriptionRequest(
                                firstActor, List.of(Long.MAX_VALUE)))))
                .andExpect(status().isNotFound());
        assertThat(prescriptionRepository.count()).isZero();
        assertThat(prescriptionItemRepository.count()).isZero();
    }

    @Test
    @DisplayName("Patient and doctor prescription lists and individual reads enforce ownership")
    void prescriptionReadOwnershipIsEnforced() throws Exception {
        String adminToken = registerAndLogin("m13.read.admin@example.com", Role.ADMIN);
        TestActor firstActor = createActor(adminToken, "m13.read.one", 4);
        TestActor secondActor = createActor(adminToken, "m13.read.two", 5);
        Medication medication = createMedication("Read Test Medication");
        Long firstPrescription = createPrescription(firstActor, List.of(medication.getId()));
        Long secondPrescription = createPrescription(secondActor, List.of(medication.getId()));

        mockMvc.perform(get("/api/patients/{patientId}/prescriptions", firstActor.patientId())
                        .header("Authorization", bearer(secondActor.patientToken())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/prescriptions/{id}", firstPrescription)
                        .header("Authorization", bearer(secondActor.patientToken())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/prescriptions/{id}", firstPrescription)
                        .header("Authorization", bearer(secondActor.doctorToken())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/doctors/{doctorId}/prescriptions", firstActor.doctorId())
                        .header("Authorization", bearer(secondActor.doctorToken())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/prescriptions/{id}", Long.MAX_VALUE)
                        .header("Authorization", bearer(firstActor.patientToken())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/prescriptions/{id}", secondPrescription)
                        .header("Authorization", bearer(secondActor.patientToken())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Only the authorized doctor can replace prescription fields and items")
    void updateIsDoctorScopedAndReplacesItemsTransactionally() throws Exception {
        String adminToken = registerAndLogin("m13.update.admin@example.com", Role.ADMIN);
        TestActor firstActor = createActor(adminToken, "m13.update.one", 4);
        TestActor secondActor = createActor(adminToken, "m13.update.two", 5);
        Medication firstMedication = createMedication("Old Medication");
        Medication secondMedication = createMedication("Replacement Medication");
        Long prescriptionId = createPrescription(firstActor, List.of(firstMedication.getId()));

        mockMvc.perform(put("/api/prescriptions/{id}", prescriptionId)
                        .header("Authorization", bearer(firstActor.patientToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(prescriptionRequest(firstActor, List.of(secondMedication.getId())))))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/prescriptions/{id}", prescriptionId)
                        .header("Authorization", bearer(secondActor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(prescriptionRequest(firstActor, List.of(secondMedication.getId())))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/prescriptions/{id}", prescriptionId)
                        .header("Authorization", bearer(firstActor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(prescriptionRequest(
                                firstActor, List.of(secondMedication.getId()), PrescriptionStatus.COMPLETED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].medicationId").value(secondMedication.getId()));

        assertThat(prescriptionItemRepository.findByPrescriptionId(prescriptionId))
                .hasSize(1)
                .allSatisfy(item -> assertThat(item.getMedicationId()).isEqualTo(secondMedication.getId()));

        mockMvc.perform(put("/api/prescriptions/{id}", prescriptionId)
                        .header("Authorization", bearer(firstActor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(prescriptionRequest(
                                firstActor, secondActor.patientId(), firstActor.doctorId(),
                                firstActor.encounterId(), List.of(secondMedication.getId()),
                                PrescriptionStatus.ACTIVE))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/prescriptions/{id}", Long.MAX_VALUE)
                        .header("Authorization", bearer(firstActor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(prescriptionRequest(firstActor, List.of(secondMedication.getId())))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Prescription and item payload validation rejects missing fields and invalid enum values")
    void requestValidationIsApplied() throws Exception {
        String adminToken = registerAndLogin("m13.validation.admin@example.com", Role.ADMIN);
        TestActor actor = createActor(adminToken, "m13.validation.one", 4);

        mockMvc.perform(post("/api/prescriptions")
                        .header("Authorization", bearer(actor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/prescriptions")
                        .header("Authorization", bearer(actor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":1,"doctorId":1,"encounterId":1,"prescribedDate":"2026-10-03",
                                 "status":"INVALID","items":[]}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/prescriptions")
                        .header("Authorization", bearer(actor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":1,"doctorId":1,"encounterId":1,"prescribedDate":"2026-10-03",
                                 "status":"ACTIVE","items":[{"medicationId":1}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    private PrescriptionRequest prescriptionRequest(TestActor actor, List<Long> medicationIds, PrescriptionStatus status) {
        return prescriptionRequest(
                actor, actor.patientId(), actor.doctorId(), actor.encounterId(), medicationIds, status);
    }

    private record TestActor(
            String patientToken,
            String doctorToken,
            Long patientId,
            Long doctorId,
            Long encounterId
    ) {
    }
}
