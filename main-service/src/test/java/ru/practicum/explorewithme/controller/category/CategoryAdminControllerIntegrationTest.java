package ru.practicum.explorewithme.controller.category;

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
import ru.practicum.explorewithme.dto.category.CategoryDto;
import ru.practicum.explorewithme.dto.category.NewCategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryAdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDatabase() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "categories");
    }

    @Test
    void postCreateCategoryShouldReturnCreated() throws Exception {
        NewCategory newCategory = new NewCategory("test");

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isCreated());
    }

    @Test
    void postCreateCategoryShouldReturnBadRequestWhenNameNull() throws Exception {
        NewCategory newCategory = new NewCategory(null);

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCreateCategoryShouldReturnBadRequestWhenNameLong() throws Exception {
        NewCategory newCategory = new NewCategory("test".repeat(100));

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCreateCategoryShouldReturnBadRequestWhenNameEmpty() throws Exception {
        NewCategory newCategory = new NewCategory("");

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCreateCategoryShouldReturnBadRequestWhenNameBlank() throws Exception {
        NewCategory newCategory = new NewCategory("    ");

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCreateCategoryShouldReturnConflictWhenCreateNotExistName() throws Exception {
        NewCategory newCategory = new NewCategory("test");
        NewCategory newCategory2 = new NewCategory("test");

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory2)))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteCategoryShouldReturnNoContent() throws Exception {
        NewCategory newCategory = new NewCategory("test");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        CategoryDto categoryDto = objectMapper.readValue(responseBody, CategoryDto.class);

        mockMvc.perform(MockMvcRequestBuilders.delete("/admin/categories/{catId}",
                                categoryDto.id())
                        .contentType("application/json"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCategoryShouldReturnNotFoundWhenIncorrectId() throws Exception {
        NewCategory newCategory = new NewCategory("test");

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.delete("/admin/categories/{catId}", 999999L)
                        .contentType("application/json"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCategoryShouldReturnOk() throws Exception {
        NewCategory newCategory = new NewCategory("test");
        NewCategory newCategory2 = new NewCategory("test2");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        CategoryDto categoryDto = objectMapper.readValue(responseBody, CategoryDto.class);

        MvcResult updateResult = mockMvc.perform(MockMvcRequestBuilders.patch("/admin/categories/{catId}",
                                categoryDto.id())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory2)))
                .andExpect(status().isOk())
                .andReturn();

        String resultBody = updateResult.getResponse().getContentAsString();

        CategoryDto resultCategoryDto = objectMapper.readValue(resultBody, CategoryDto.class);

        assertEquals(newCategory2.getName(), resultCategoryDto.name());
    }

    @Test
    void updateCategoryShouldReturnConflictWhenUpdateNotExistName() throws Exception {
        NewCategory newCategory = new NewCategory("test");
        NewCategory newCategory2 = new NewCategory("test2");
        NewCategory newCategory3 = new NewCategory("test2");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        CategoryDto categoryDto = objectMapper.readValue(responseBody, CategoryDto.class);

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory2)))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.patch("/admin/categories/{catId}",
                                categoryDto.id())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory3)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateCategoryShouldReturnNotFoundWhenUpdateByIncorrectId() throws Exception {
        NewCategory newCategory = new NewCategory("test");
        NewCategory newCategory2 = new NewCategory("test2");
        NewCategory newCategory3 = new NewCategory("test3");

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory2)))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.patch("/admin/categories/{catId}", 100500L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory3)))
                .andExpect(status().isNotFound());
    }
}