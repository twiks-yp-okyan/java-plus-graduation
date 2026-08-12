package ru.practicum.explorewithme.controller.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.practicum.explorewithme.dto.user.NewUserRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDatabase() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "compilation_events",
                "requests",
                "events",
                "compilations",
                "locations",
                "categories",
                "users");
    }

    @Test
    void postCreateUserShouldReturnCreated() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setEmail("user@example.com");
        request.setName("User Name");

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void postCreateUserShouldReturnBadRequestWhenEmailInvalid() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setEmail("invalid-email");
        request.setName("User Name");

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCreateUserShouldReturnBadRequestWhenNameBlank() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setEmail("user@example.com");
        request.setName("   ");

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCreateUserShouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setEmail("user@example.com");
        request.setName("User Name");

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteUserShouldReturnNoContent() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setEmail("user@example.com");
        request.setName("User Name");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/admin/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> userMap = objectMapper.readValue(responseBody, new TypeReference<>() {
        });
        Long userId = ((Number) userMap.get("id")).longValue();

        mockMvc.perform(MockMvcRequestBuilders.delete("/admin/users/{id}", userId)
                        .contentType("application/json"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUserShouldReturnNotFoundWhenIncorrectId() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setEmail("user@example.com");
        request.setName("User Name");

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.delete("/admin/users/{id}", 999999L)
                        .contentType("application/json"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUsersShouldReturnList_whenUsersExist() throws Exception {
        NewUserRequest request1 = new NewUserRequest();
        request1.setEmail("user1@example.com");
        request1.setName("User One");

        NewUserRequest request2 = new NewUserRequest();
        request2.setEmail("user2@example.com");
        request2.setName("User Two");

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/admin/users")
                        .param("from", "0")
                        .param("size", "10")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<?> users = objectMapper.readValue(responseBody, List.class);

        assertEquals(2, users.size());
    }
}
