package com.careconnect.controller;

import com.careconnect.dto.AppointmentRequest;
import com.careconnect.dto.LoginRequest;
import com.careconnect.entity.AppointmentStatus;
import com.careconnect.entity.Role;
import com.careconnect.repository.AppointmentRepository;
import com.careconnect.repository.DoctorRepository;
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
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AppointmentControllerTest {

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
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        doctorRepository.deleteAll();
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
    @DisplayName("Patient can book, view, list and cancel their appointment")
    void patientBookViewListCancel() throws Exception {
        String patientToken = registerAndLogin("pat.app@example.com", "Password123", Role.PATIENT);
        String doctorUserToken = registerAndLogin("doc.app@example.com", "Password123", Role.DOCTOR);
        String adminToken = registerAndLogin("admin.app@example.com", "Password123", Role.ADMIN);

        // create patient profile and doctor profile
        Long patientUserId = userRepository.findByEmail("pat.app@example.com").get().getId();
        Long doctorUserId = userRepository.findByEmail("doc.app@example.com").get().getId();

        var pReq = new com.careconnect.dto.PatientRequest(patientUserId, "PFirst", "PLast", null, null, null, null, null, null);
        var pRes = objectMapper.readTree(mockMvc.perform(post("/api/patients").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(pReq))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        Long patientId = pRes.get("id").asLong();

        // create department and doctor
        var deptReq = new com.careconnect.dto.DepartmentRequest("Cardiology", null);
        var deptRes = objectMapper.readTree(mockMvc.perform(post("/api/departments").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(deptReq))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        Long deptId = deptRes.get("id").asLong();

        var docReq = new com.careconnect.dto.DoctorRequest(doctorUserId, deptId, "Doc", "Tor", null, "LIC123", null);
        var docRes = objectMapper.readTree(mockMvc.perform(post("/api/doctors").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(docReq))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        Long doctorId = docRes.get("id").asLong();

        // Patient books appointment
        AppointmentRequest apptReq = new AppointmentRequest(patientId, doctorId, LocalDate.now().plusDays(1), LocalTime.of(10,0), "Checkup", "Notes");
        var createRes = objectMapper.readTree(mockMvc.perform(post("/api/appointments").header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(apptReq))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        Long apptId = createRes.get("id").asLong();
        assertThat(createRes.get("patientId").asLong()).isEqualTo(patientId);
        assertThat(createRes.get("doctorId").asLong()).isEqualTo(doctorId);
        assertThat(createRes.get("status").asText()).isEqualTo(AppointmentStatus.SCHEDULED.name());

        // Patient views appointment
        mockMvc.perform(get("/api/appointments/" + apptId).header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(apptId));

        // Patient lists their appointments
        mockMvc.perform(get("/api/appointments/patient/" + patientId).header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(apptId));

        // Patient cancels their appointment
        mockMvc.perform(delete("/api/appointments/" + apptId).header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isNoContent());

        // After cancel, listing by doctor should not treat as active conflict when scheduling another
        AppointmentRequest apptReq2 = new AppointmentRequest(patientId, doctorId, LocalDate.now().plusDays(1), LocalTime.of(10,0), "Followup", null);
        mockMvc.perform(post("/api/appointments").header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(apptReq2))).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Admin-only dashboard reads return safe users and all appointments")
    void adminCanReadUsersAndAppointmentsOnly() throws Exception {
        String adminToken = registerAndLogin("admin.read@example.com", "Password123", Role.ADMIN);
        String patientToken = registerAndLogin("patient.read@example.com", "Password123", Role.PATIENT);

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin.read@example.com"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[0].password").doesNotExist());

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/appointments").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/appointments").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Patient cannot create or view another patient's appointment")
    void patientCannotCreateOrViewAnother() throws Exception {
        String patient1Token = registerAndLogin("pat1.app@example.com", "Password123", Role.PATIENT);
        String patient2Token = registerAndLogin("pat2.app@example.com", "Password123", Role.PATIENT);
        String adminToken = registerAndLogin("admin2.app@example.com", "Password123", Role.ADMIN);
        String docToken = registerAndLogin("doc2.app@example.com", "Password123", Role.DOCTOR);

        Long p1User = userRepository.findByEmail("pat1.app@example.com").get().getId();
        Long p2User = userRepository.findByEmail("pat2.app@example.com").get().getId();
        Long docUser = userRepository.findByEmail("doc2.app@example.com").get().getId();

        var p1Req = new com.careconnect.dto.PatientRequest(p1User, "P1", "One", null, null, null, null, null, null);
        Long p1 = objectMapper.readTree(mockMvc.perform(post("/api/patients").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(p1Req))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        var p2Req = new com.careconnect.dto.PatientRequest(p2User, "P2", "Two", null, null, null, null, null, null);
        Long p2 = objectMapper.readTree(mockMvc.perform(post("/api/patients").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(p2Req))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        // create dept and doctor
        var deptReq = new com.careconnect.dto.DepartmentRequest("Derm", null);
        Long deptId = objectMapper.readTree(mockMvc.perform(post("/api/departments").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(deptReq))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        var docReq = new com.careconnect.dto.DoctorRequest(docUser, deptId, "Doc", "Two", null, "LIC456", null);
        Long docId = objectMapper.readTree(mockMvc.perform(post("/api/doctors").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(docReq))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        // patient1 books
        AppointmentRequest apptReq = new AppointmentRequest(p1, docId, LocalDate.now().plusDays(2), LocalTime.of(11,0), "X", null);
        Long apptId = objectMapper.readTree(mockMvc.perform(post("/api/appointments").header("Authorization", "Bearer " + patient1Token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(apptReq))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        // patient2 tries to view patient1's appointment
        mockMvc.perform(get("/api/appointments/" + apptId).header("Authorization", "Bearer " + patient2Token)).andExpect(status().isForbidden());

        // patient2 tries to create appointment for patient1
        AppointmentRequest badCreate = new AppointmentRequest(p1, docId, LocalDate.now().plusDays(3), LocalTime.of(9,0), "Bad", null);
        mockMvc.perform(post("/api/appointments").header("Authorization", "Bearer " + patient2Token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(badCreate))).andExpect(status().isForbidden());

        // unauthenticated request
        mockMvc.perform(get("/api/appointments/" + apptId)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Doctor can list and view their appointments but cannot modify others')")
    void doctorListView() throws Exception {
        String adminToken = registerAndLogin("adm3@app.com", "Password123", Role.ADMIN);
        String doc1Token = registerAndLogin("doc3@app.com", "Password123", Role.DOCTOR);
        String doc2Token = registerAndLogin("doc4@app.com", "Password123", Role.DOCTOR);
        String patToken = registerAndLogin("pat3@app.com", "Password123", Role.PATIENT);

        Long doc1User = userRepository.findByEmail("doc3@app.com").get().getId();
        Long doc2User = userRepository.findByEmail("doc4@app.com").get().getId();
        Long patUser = userRepository.findByEmail("pat3@app.com").get().getId();

        var pReq = new com.careconnect.dto.PatientRequest(patUser, "Px", "Y", null, null, null, null, null, null);
        Long patId = objectMapper.readTree(mockMvc.perform(post("/api/patients").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(pReq))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        var deptReq = new com.careconnect.dto.DepartmentRequest("Ortho", null);
        Long deptId = objectMapper.readTree(mockMvc.perform(post("/api/departments").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(deptReq))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        var doc1Req = new com.careconnect.dto.DoctorRequest(doc1User, deptId, "D1", "One", null, "L1", null);
        Long d1 = objectMapper.readTree(mockMvc.perform(post("/api/doctors").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(doc1Req))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        var doc2Req = new com.careconnect.dto.DoctorRequest(doc2User, deptId, "D2", "Two", null, "L2", null);
        Long d2 = objectMapper.readTree(mockMvc.perform(post("/api/doctors").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(doc2Req))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        // create appts for both doctors
        AppointmentRequest a1 = new AppointmentRequest(patId, d1, LocalDate.now().plusDays(4), LocalTime.of(9,0), "A1", null);
        AppointmentRequest a2 = new AppointmentRequest(patId, d2, LocalDate.now().plusDays(4), LocalTime.of(10,0), "A2", null);
        Long ap1 = objectMapper.readTree(mockMvc.perform(post("/api/appointments").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(a1))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        Long ap2 = objectMapper.readTree(mockMvc.perform(post("/api/appointments").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(a2))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        // doc1 lists their appts
        mockMvc.perform(get("/api/appointments/doctor/" + d1).header("Authorization", "Bearer " + doc1Token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(ap1));

        // doc2 cannot view doc1's appointment detail
        mockMvc.perform(get("/api/appointments/" + ap1).header("Authorization", "Bearer " + doc2Token)).andExpect(status().isForbidden());

        // doc1 cannot change doctorId to impersonate another doctor via update
        AppointmentRequest badUpd = new AppointmentRequest(patId, d2, LocalDate.now().plusDays(5), LocalTime.of(11,0), "X", null);
        mockMvc.perform(put("/api/appointments/" + ap1).header("Authorization", "Bearer " + doc1Token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(badUpd))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admin can manage appointments and conflicts are enforced")
    void adminManageAndConflicts() throws Exception {
        String admin = registerAndLogin("adm4@app.com", "Password123", Role.ADMIN);
        String pat = registerAndLogin("pat4@app.com", "Password123", Role.PATIENT);
        String doc = registerAndLogin("doc5@app.com", "Password123", Role.DOCTOR);

        Long patUser = userRepository.findByEmail("pat4@app.com").get().getId();
        Long docUser = userRepository.findByEmail("doc5@app.com").get().getId();

        Long pId = objectMapper.readTree(mockMvc.perform(post("/api/patients").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new com.careconnect.dto.PatientRequest(patUser, "PF", "PL", null, null, null, null, null, null)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        Long deptId = objectMapper.readTree(mockMvc.perform(post("/api/departments").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new com.careconnect.dto.DepartmentRequest("ENT", null)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        Long dId = objectMapper.readTree(mockMvc.perform(post("/api/doctors").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new com.careconnect.dto.DoctorRequest(docUser, deptId, "Doc", "A", null, "LIC999", null)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        // create an appointment
        AppointmentRequest a = new AppointmentRequest(pId, dId, LocalDate.now().plusDays(6), LocalTime.of(14,0), "S", null);
        mockMvc.perform(post("/api/appointments").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(a))).andExpect(status().isCreated());

        // try to create conflicting appointment for same doctor/date/time
        mockMvc.perform(post("/api/appointments").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new AppointmentRequest(pId, dId, LocalDate.now().plusDays(6), LocalTime.of(14,0), "C", null)))).andExpect(status().isBadRequest());

        // Nonexistent patient/doctor
        mockMvc.perform(post("/api/appointments").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new AppointmentRequest(9999L, dId, LocalDate.now().plusDays(7), LocalTime.of(15,0), "X", null))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/appointments").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new AppointmentRequest(pId, 9999L, LocalDate.now().plusDays(7), LocalTime.of(15,0), "X", null))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Invalid data returns validation errors and nonexistent appointment 404")
    void validationAndNotFound() throws Exception {
        String admin = registerAndLogin("adm5@app.com", "Password123", Role.ADMIN);

        // missing fields
        mockMvc.perform(post("/api/appointments").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        // get nonexistent
        mockMvc.perform(get("/api/appointments/99999").header("Authorization", "Bearer " + admin)).andExpect(status().isNotFound());
    }
}
