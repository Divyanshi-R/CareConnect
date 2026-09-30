package com.careconnect.controller;

import com.careconnect.dto.DoctorRequest;
import com.careconnect.dto.DepartmentRequest;
import com.careconnect.entity.Role;
import com.careconnect.repository.DoctorRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        doctorRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndLogin(String email, String password, Role role) throws Exception {
        var register = new com.careconnect.dto.RegisterRequest(email, password, role);
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        var login = new com.careconnect.dto.LoginRequest(email, password);
        MvcResult res = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk()).andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    @Test
    @DisplayName("Admin can create/list/view/update/delete doctors; patients cannot modify")
    void adminCrudDoctors() throws Exception {
        String adminToken = registerAndLogin("doc.admin@example.com", "Password123", Role.ADMIN);
        String deptAdminToken = adminToken;

        // create a department first
        DepartmentRequest deptReq = new DepartmentRequest("Neurology", "Neuro");
        MvcResult deptRes = mockMvc.perform(post("/api/departments").header("Authorization", "Bearer " + deptAdminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(deptReq)))
                .andExpect(status().isCreated()).andReturn();
        JsonNode deptJson = objectMapper.readTree(deptRes.getResponse().getContentAsString());
        Long deptId = deptJson.get("id").asLong();

        // create a user with DOCTOR role
        String doctorUserEmail = "dr.unique@example.com";
        String doctorToken = registerAndLogin(doctorUserEmail, "Password123", Role.DOCTOR);
        Long userId = userRepository.findByEmail(doctorUserEmail).get().getId();

        DoctorRequest drReq = new DoctorRequest(userId, deptId, "John", "Doe", "Neuro", "LIC-12345", "555-000");
        MvcResult createRes = mockMvc.perform(post("/api/doctors").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(drReq)))
                .andExpect(status().isCreated()).andReturn();

        JsonNode created = objectMapper.readTree(createRes.getResponse().getContentAsString());
        Long doctorId = created.get("id").asLong();

        // list and view
        mockMvc.perform(get("/api/doctors").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/doctors/" + doctorId).header("Authorization", "Bearer " + doctorToken)).andExpect(status().isOk());

        // update
        DoctorRequest upd = new DoctorRequest(userId, deptId, "John", "Smith", "Neuro", "LIC-12345", "555-111");
        mockMvc.perform(put("/api/doctors/" + doctorId).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(upd)))
                .andExpect(status().isOk());

        // delete
        mockMvc.perform(delete("/api/doctors/" + doctorId).header("Authorization", "Bearer " + adminToken)).andExpect(status().isNoContent());

        // patient cannot create
        String patientToken = registerAndLogin("doc.patient@example.com", "Password123", Role.PATIENT);
        mockMvc.perform(post("/api/doctors").header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(drReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Duplicate license number rejected and department/user validations")
    void licenseAndReferencesValidation() throws Exception {
        String adminToken = registerAndLogin("doc.admin2@example.com", "Password123", Role.ADMIN);

        DepartmentRequest deptReq = new DepartmentRequest("Oncology", "Oncology");
        MvcResult deptRes = mockMvc.perform(post("/api/departments").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(deptReq)))
                .andExpect(status().isCreated()).andReturn();
        Long deptId = objectMapper.readTree(deptRes.getResponse().getContentAsString()).get("id").asLong();

        String docEmail1 = "dup1@example.com";
        String docEmail2 = "dup2@example.com";
        registerAndLogin(docEmail1, "Password123", Role.DOCTOR);
        registerAndLogin(docEmail2, "Password123", Role.DOCTOR);
        Long user1 = userRepository.findByEmail(docEmail1).get().getId();
        Long user2 = userRepository.findByEmail(docEmail2).get().getId();

        DoctorRequest r1 = new DoctorRequest(user1, deptId, "A", "B", "Spec", "LIC-DUP", "111");
        mockMvc.perform(post("/api/doctors").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(r1)))
                .andExpect(status().isCreated());

        DoctorRequest r2 = new DoctorRequest(user2, deptId, "C", "D", "Spec", "LIC-DUP", "222");
        mockMvc.perform(post("/api/doctors").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(r2)))
                .andExpect(status().isBadRequest());
    }
}
