package ru.practicum.explorewithme.controller.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.practicum.explorewithme.client.StatClient;
import ru.practicum.explorewithme.dto.ViewStats;
import ru.practicum.explorewithme.dto.event.NewEventDto;
import ru.practicum.explorewithme.service.request.RequestService;
import ru.practicum.explorewithme.dto.event.UpdateEventUserRequest;
import ru.practicum.explorewithme.dto.location.LocationDto;
import ru.practicum.explorewithme.dto.user.NewUserRequest;
import ru.practicum.explorewithme.dto.category.NewCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventPrivateControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private StatClient statClient;

    @MockBean
    private RequestService requestService;

    @BeforeEach
    void setUp() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate,
                "events",
                "users",
                "categories",
                "locations"
        );
        // Mock StatClient so tests do not depend on stats-service
        when(statClient.getStat(any(LocalDateTime.class), any(LocalDateTime.class), any(List.class), anyBoolean()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<String> uris = invocation.getArgument(2);
                    return uris.stream()
                            .map(uri -> new ViewStats("ewm", uri, 0L))
                            .collect(Collectors.toList());
                });
        // Mock RequestService so event list/get responses have confirmedRequests for all event ids
        when(requestService.countRequestsByEventId(anyLong())).thenReturn(0L);
        when(requestService.countRequestsByEventIds(anySet()))
                .thenAnswer(invocation -> {
                    Set<Long> ids = invocation.getArgument(0);
                    return ids.stream().collect(Collectors.toMap(id -> id, id -> 0L));
                });
    }

    private Long createUser() throws Exception {
        NewUserRequest request = new NewUserRequest();
        request.setEmail("user@example.com");
        request.setName("User Name");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/admin/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> userMap = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );

        return ((Number) userMap.get("id")).longValue();
    }

    private Long createCategory() throws Exception {
        NewCategory newCategory = new NewCategory("test-category");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> categoryMap = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );

        return ((Number) categoryMap.get("id")).longValue();
    }

    private NewEventDto buildValidNewEventDto(Long categoryId) {
        return NewEventDto.builder()
                .annotation("A".repeat(20))
                .category(categoryId)
                .description("D".repeat(20))
                .eventDate("2099-01-01 12:00:00")
                .location(new LocationDto(10.0, 20.0))
                .paid(false)
                .participantLimit(100)
                .requestModeration(true)
                .title("Valid event title")
                .build();
    }

    @Test
    void postCreateEventShouldReturnCreated() throws Exception {
        Long userId = createUser();
        Long categoryId = createCategory();
        NewEventDto newEvent = buildValidNewEventDto(categoryId);

        mockMvc.perform(MockMvcRequestBuilders.post("/users/{userId}/events", userId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isCreated());
    }

    @Test
    void postCreateEventShouldReturnBadRequestWhenInvalidBody() throws Exception {
        Long userId = createUser();
        Long categoryId = createCategory();
        NewEventDto invalidEvent = NewEventDto.builder()
                .annotation("short")
                .category(categoryId)
                .description("short")
                .eventDate("2099-01-01 12:00:00")
                .location(new LocationDto(10.0, 20.0))
                .title("ab")
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/users/{userId}/events", userId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEventByUserIdAndIdShouldReturnEvent() throws Exception {
        Long userId = createUser();
        Long categoryId = createCategory();
        NewEventDto newEvent = buildValidNewEventDto(categoryId);

        MvcResult createResult = mockMvc.perform(MockMvcRequestBuilders.post("/users/{userId}/events", userId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> createdEvent = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        Long eventId = ((Number) createdEvent.get("id")).longValue();

        mockMvc.perform(MockMvcRequestBuilders.get("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType("application/json"))
                .andExpect(status().isOk());
    }

    @Test
    void getEventByUserIdAndIdShouldReturnNotFoundWhenIncorrectId() throws Exception {
        Long userId = createUser();

        mockMvc.perform(MockMvcRequestBuilders.get("/users/{userId}/events/{eventId}", userId, 999999L)
                        .contentType("application/json"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEventsByUserIdShouldReturnList() throws Exception {
        Long userId = createUser();
        Long categoryId = createCategory();
        NewEventDto newEvent1 = buildValidNewEventDto(categoryId);
        NewEventDto newEvent2 = buildValidNewEventDto(categoryId);

        mockMvc.perform(MockMvcRequestBuilders.post("/users/{userId}/events", userId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newEvent1)))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.post("/users/{userId}/events", userId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newEvent2)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/users/{userId}/events", userId)
                        .param("from", "0")
                        .param("size", "10")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<?> events = objectMapper.readValue(responseBody, List.class);

        assertEquals(2, events.size());
    }

    @Test
    void patchUpdateEventShouldReturnOk() throws Exception {
        Long userId = createUser();
        Long categoryId = createCategory();
        NewEventDto newEvent = buildValidNewEventDto(categoryId);

        MvcResult createResult = mockMvc.perform(MockMvcRequestBuilders.post("/users/{userId}/events", userId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> createdEvent = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        Long eventId = ((Number) createdEvent.get("id")).longValue();

        UpdateEventUserRequest updateRequest = UpdateEventUserRequest.builder()
                .title("Updated title")
                .build();

        MvcResult updateResult = mockMvc.perform(MockMvcRequestBuilders.patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> updatedEvent = objectMapper.readValue(
                updateResult.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );

        assertEquals("Updated title", updatedEvent.get("title"));
    }

    @Test
    void patchUpdateEventShouldReturnBadRequestWhenInvalidBody() throws Exception {
        Long userId = createUser();
        Long categoryId = createCategory();
        NewEventDto newEvent = buildValidNewEventDto(categoryId);

        MvcResult createResult = mockMvc.perform(MockMvcRequestBuilders.post("/users/{userId}/events", userId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> createdEvent = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        Long eventId = ((Number) createdEvent.get("id")).longValue();

        UpdateEventUserRequest invalidUpdate = UpdateEventUserRequest.builder()
                .title("ab")
                .build();

        mockMvc.perform(MockMvcRequestBuilders.patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidUpdate)))
                .andExpect(status().isBadRequest());
    }
}

