package com.careconnect.controller;

import com.careconnect.dto.DepartmentRequest;
import com.careconnect.entity.Role;
import com.careconnect.repository.DepartmentRepository;
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
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        departmentRepository.deleteAll();
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
    @DisplayName("Admin can CRUD departments; others limited")
    void adminCrudDepartments() throws Exception {
        String adminToken = registerAndLogin("dept.admin@example.com", "Password123", Role.ADMIN);
        String doctorToken = registerAndLogin("dept.doc@example.com", "Password123", Role.DOCTOR);
        String patientToken = registerAndLogin("dept.patient@example.com", "Password123", Role.PATIENT);

        DepartmentRequest req = new DepartmentRequest("Cardiology", "Heart department");
        MvcResult createRes = mockMvc.perform(post("/api/departments").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();

        JsonNode created = objectMapper.readTree(createRes.getResponse().getContentAsString());
        Long id = created.get("id").asLong();

        // list/view by doctor and patient
        mockMvc.perform(get("/api/departments").header("Authorization", "Bearer " + doctorToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/departments/" + id).header("Authorization", "Bearer " + patientToken)).andExpect(status().isOk());

        // update by admin
        DepartmentRequest upd = new DepartmentRequest("Cardio", "Updated");
        mockMvc.perform(put("/api/departments/" + id).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(upd)))
                .andExpect(status().isOk());

        // delete by admin
        mockMvc.perform(delete("/api/departments/" + id).header("Authorization", "Bearer " + adminToken)).andExpect(status().isNoContent());

        // unauthorized create attempt
        mockMvc.perform(post("/api/departments").header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // unauthenticated access
        mockMvc.perform(get("/api/departments")).andExpect(status().isUnauthorized());
    }
}
