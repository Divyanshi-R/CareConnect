package com.careconnect.controller;

import com.careconnect.dto.ClinicalOrderRequest;
import com.careconnect.dto.DepartmentRequest;
import com.careconnect.dto.DoctorRequest;
import com.careconnect.dto.EncounterRequest;
import com.careconnect.dto.LoginRequest;
import com.careconnect.dto.PatientRequest;
import com.careconnect.dto.RegisterRequest;
import com.careconnect.entity.ClinicalOrder;
import com.careconnect.entity.OrderPriority;
import com.careconnect.entity.OrderStatus;
import com.careconnect.entity.OrderType;
import com.careconnect.entity.Role;
import com.careconnect.repository.AppointmentRepository;
import com.careconnect.repository.ClinicalNoteRepository;
import com.careconnect.repository.ClinicalOrderRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClinicalOrderControllerTest {

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
    private ClinicalOrderRepository clinicalOrderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanRepositories() {
        clinicalOrderRepository.deleteAll();
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

    @Test
    @DisplayName("Doctor creates an order with server-controlled status and timestamps")
    void doctorCreatesAndRetrievesOwnOrder() throws Exception {
        String adminToken = registerAndLogin("m14.create.admin@example.com", Role.ADMIN);
        TestActor actor = createActor(adminToken, "m14.create.one", 3);

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(actor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(actor, OrderStatus.COMPLETED))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(actor.patientId()))
                .andExpect(jsonPath("$.doctorId").value(actor.doctorId()))
                .andExpect(jsonPath("$.encounterId").value(actor.encounterId()))
                .andExpect(jsonPath("$.orderType").value("LAB"))
                .andExpect(jsonPath("$.priority").value("URGENT"))
                .andExpect(jsonPath("$.status").value("ORDERED"))
                .andExpect(jsonPath("$.orderedAt").isNotEmpty())
                .andExpect(jsonPath("$.completedAt").value(nullValue()))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Long orderId = response.get("id").asLong();
        assertThat(response.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "id", "patientId", "doctorId", "encounterId", "orderType", "description",
                "priority", "status", "orderedAt", "completedAt");
        assertThat(response.toString()).doesNotContain("password", "jwt", "credentials");

        ClinicalOrder saved = clinicalOrderRepository.findById(orderId).orElseThrow();
        assertThat(saved.getPatientId()).isEqualTo(actor.patientId());
        assertThat(saved.getDoctorId()).isEqualTo(actor.doctorId());
        assertThat(saved.getEncounterId()).isEqualTo(actor.encounterId());
        assertThat(saved.getOrderType()).isEqualTo(OrderType.LAB);
        assertThat(saved.getPriority()).isEqualTo(OrderPriority.URGENT);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.ORDERED);
        assertThat(saved.getOrderedAt()).isNotNull();
        assertThat(saved.getCompletedAt()).isNull();

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", bearer(actor.patientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));
        mockMvc.perform(get("/api/patients/{patientId}/orders", actor.patientId())
                        .header("Authorization", bearer(actor.patientToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId));
        mockMvc.perform(get("/api/doctors/{doctorId}/orders", actor.doctorId())
                        .header("Authorization", bearer(actor.doctorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId));
    }

    @Test
    @DisplayName("Creation rejects unauthorized users and invalid encounter ownership")
    void createAuthorizationAndRelationshipsAreEnforced() throws Exception {
        String adminToken = registerAndLogin("m14.auth.admin@example.com", Role.ADMIN);
        TestActor first = createActor(adminToken, "m14.auth.one", 3);
        TestActor second = createActor(adminToken, "m14.auth.two", 4);
        String validRequest = json(request(first, OrderStatus.ORDERED));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(first.patientToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(second.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(first.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(first, second.patientId(), first.doctorId(),
                                first.encounterId(), OrderStatus.ORDERED))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(first.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(first, first.patientId(), second.doctorId(),
                                first.encounterId(), OrderStatus.ORDERED))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(first.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(first, first.patientId(), first.doctorId(),
                                Long.MAX_VALUE, OrderStatus.ORDERED))))
                .andExpect(status().isNotFound());

        assertThat(clinicalOrderRepository.count()).isZero();
    }

    @Test
    @DisplayName("Order reads enforce patient and doctor ownership for individual and list routes")
    void readOwnershipIsEnforced() throws Exception {
        String adminToken = registerAndLogin("m14.read.admin@example.com", Role.ADMIN);
        TestActor first = createActor(adminToken, "m14.read.one", 3);
        TestActor second = createActor(adminToken, "m14.read.two", 4);
        Long firstOrderId = createOrder(first);
        Long secondOrderId = createOrder(second);

        mockMvc.perform(get("/api/orders/{id}", firstOrderId)
                        .header("Authorization", bearer(second.patientToken())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders/{id}", firstOrderId)
                        .header("Authorization", bearer(second.doctorToken())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders/{id}", Long.MAX_VALUE)
                        .header("Authorization", bearer(first.patientToken())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/patients/{patientId}/orders", first.patientId())
                        .header("Authorization", bearer(second.patientToken())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/patients/{patientId}/orders", first.patientId())
                        .header("Authorization", bearer(second.doctorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/patients/{patientId}/orders", first.patientId())
                        .header("Authorization", bearer(first.doctorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(firstOrderId));

        mockMvc.perform(get("/api/doctors/{doctorId}/orders", first.doctorId())
                        .header("Authorization", bearer(second.doctorToken())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/doctors/{doctorId}/orders", first.doctorId())
                        .header("Authorization", bearer(first.patientToken())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/doctors/{doctorId}/orders", first.doctorId())
                        .header("Authorization", bearer(first.doctorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(firstOrderId));

        mockMvc.perform(get("/api/patients/{patientId}/orders", first.patientId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(firstOrderId));
        assertThat(secondOrderId).isNotEqualTo(firstOrderId);
    }

    @Test
    @DisplayName("Only the encounter doctor can update orders and valid status changes set completion time")
    void updateIsDoctorScopedAndValidatesTransitionsAndIdentity() throws Exception {
        String adminToken = registerAndLogin("m14.update.admin@example.com", Role.ADMIN);
        TestActor first = createActor(adminToken, "m14.update.one", 3);
        TestActor second = createActor(adminToken, "m14.update.two", 4);
        Long orderId = createOrder(first);

        mockMvc.perform(put("/api/orders/{id}", orderId)
                        .header("Authorization", bearer(first.patientToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(first, OrderStatus.IN_PROGRESS))))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/orders/{id}", orderId)
                        .header("Authorization", bearer(second.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(first, OrderStatus.IN_PROGRESS))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/orders/{id}", orderId)
                        .header("Authorization", bearer(first.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(first, OrderStatus.IN_PROGRESS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(put("/api/orders/{id}", orderId)
                        .header("Authorization", bearer(first.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(first, OrderStatus.COMPLETED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        ClinicalOrder completed = clinicalOrderRepository.findById(orderId).orElseThrow();
        assertThat(completed.getCompletedAt()).isNotNull();
        mockMvc.perform(put("/api/orders/{id}", orderId)
                        .header("Authorization", bearer(first.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(first, OrderStatus.ORDERED))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/orders/{id}", orderId)
                        .header("Authorization", bearer(first.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(first, second.patientId(), first.doctorId(),
                                first.encounterId(), OrderStatus.COMPLETED))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", bearer(first.doctorToken())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Doctor cancellation supports ordered and in-progress orders only")
    void cancellationRulesAreEnforced() throws Exception {
        String adminToken = registerAndLogin("m14.cancel.admin@example.com", Role.ADMIN);
        TestActor actor = createActor(adminToken, "m14.cancel.one", 3);
        Long orderedId = createOrder(actor);
        Long inProgressId = createOrder(actor);

        mockMvc.perform(post("/api/orders/{id}/cancel", orderedId)
                        .header("Authorization", bearer(actor.patientToken())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/orders/{id}/cancel", orderedId)
                        .header("Authorization", bearer(actor.doctorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.completedAt").value(nullValue()));
        mockMvc.perform(post("/api/orders/{id}/cancel", orderedId)
                        .header("Authorization", bearer(actor.doctorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(put("/api/orders/{id}", inProgressId)
                        .header("Authorization", bearer(actor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(actor, OrderStatus.IN_PROGRESS))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/orders/{id}/cancel", inProgressId)
                        .header("Authorization", bearer(actor.doctorToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.completedAt").value(nullValue()));
    }

    @Test
    @DisplayName("Invalid order fields and enum values are rejected")
    void requestValidationIsApplied() throws Exception {
        String adminToken = registerAndLogin("m14.validation.admin@example.com", Role.ADMIN);
        TestActor actor = createActor(adminToken, "m14.validation.one", 3);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(actor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(actor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":1,"doctorId":1,"encounterId":1,"orderType":"INVALID",
                                 "description":"Test","priority":"ROUTINE","status":"ORDERED"}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(actor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ClinicalOrderRequest(
                                actor.patientId(), actor.doctorId(), actor.encounterId(),
                                OrderType.LAB, "  ", OrderPriority.ROUTINE, OrderStatus.ORDERED))))
                .andExpect(status().isBadRequest());
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
                        .content(json(new DepartmentRequest("M14 " + suffix, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long departmentId = objectMapper.readTree(departmentResponse).get("id").asLong();

        String doctorResponse = mockMvc.perform(post("/api/doctors")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new DoctorRequest(
                                doctorUserId, departmentId, "Test", "Doctor", null,
                                "M14-LIC-" + suffix, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long doctorId = objectMapper.readTree(doctorResponse).get("id").asLong();

        LocalDate appointmentDate = LocalDate.now().plusDays(dayOffset);
        String appointmentResponse = mockMvc.perform(post("/api/appointments")
                        .header("Authorization", bearer(patientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":%d,"doctorId":%d,"appointmentDate":"%s",
                                 "appointmentTime":"10:00:00","reason":"Consultation","notes":"Initial"}
                                """.formatted(patientId, doctorId, appointmentDate)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long appointmentId = objectMapper.readTree(appointmentResponse).get("id").asLong();

        String encounterResponse = mockMvc.perform(post("/api/encounters")
                        .header("Authorization", bearer(patientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new EncounterRequest(
                                patientId, doctorId, appointmentId, appointmentDate,
                                "Consultation", "Assessment", "Plan", "Notes", "Vitals"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long encounterId = objectMapper.readTree(encounterResponse).get("id").asLong();

        return new TestActor(patientToken, doctorToken, patientId, doctorId, encounterId);
    }

    private Long createOrder(TestActor actor) throws Exception {
        String response = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(actor.doctorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(actor, OrderStatus.ORDERED))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private ClinicalOrderRequest request(TestActor actor, OrderStatus status) {
        return request(actor, actor.patientId(), actor.doctorId(), actor.encounterId(), status);
    }

    private ClinicalOrderRequest request(
            TestActor actor,
            Long patientId,
            Long doctorId,
            Long encounterId,
            OrderStatus status
    ) {
        return new ClinicalOrderRequest(
                patientId, doctorId, encounterId, OrderType.LAB, "Complete blood count",
                OrderPriority.URGENT, status);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
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
