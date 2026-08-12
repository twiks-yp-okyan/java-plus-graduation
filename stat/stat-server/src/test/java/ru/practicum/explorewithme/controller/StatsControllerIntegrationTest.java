package ru.practicum.explorewithme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.dto.EndpointHit;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatsControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM stat");
    }

    @Test
    void postHitShouldReturnCreated() throws Exception {
        EndpointHit endpointHit = new EndpointHit(
                null,
                "ewm-main-service",
                "/events/1",
                "192.168.0.1",
                LocalDateTime.of(2026, 3, 1, 12, 0, 0)
        );

        mockMvc.perform(post("/hit")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(endpointHit)))
                .andExpect(status().isCreated());

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stat", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void postHitShouldReturnBadRequestWhenBodyIsInvalid() throws Exception {
        EndpointHit endpointHit = new EndpointHit(
                null,
                "",
                "/events/1",
                "192.168.0.1",
                LocalDateTime.of(2026, 3, 1, 12, 0, 0)
        );

        mockMvc.perform(post("/hit")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(endpointHit)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStatsShouldReturnAggregatedValues() throws Exception {
        createHit("ewm-main-service", "/events/1", "192.168.0.1", LocalDateTime.of(2026, 3, 1, 11, 0, 0));
        createHit("ewm-main-service", "/events/1", "192.168.0.1", LocalDateTime.of(2026, 3, 1, 11, 5, 0));
        createHit("ewm-main-service", "/events/2", "192.168.0.2", LocalDateTime.of(2026, 3, 1, 11, 10, 0));

        mockMvc.perform(get("/stats")
                        .param("start", "2026-03-01 10:00:00")
                        .param("end", "2026-03-01 12:00:00")
                        .param("uris", "/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("ewm-main-service"))
                .andExpect(jsonPath("$[0].uri").value("/events/1"))
                .andExpect(jsonPath("$[0].hits").value(2));
    }

    @Test
    void getStatsShouldReturnUniqueValues() throws Exception {
        createHit("ewm-main-service", "/events/1", "192.168.0.1", LocalDateTime.of(2026, 3, 1, 11, 0, 0));
        createHit("ewm-main-service", "/events/1", "192.168.0.1", LocalDateTime.of(2026, 3, 1, 11, 5, 0));
        createHit("ewm-main-service", "/events/1", "192.168.0.2", LocalDateTime.of(2026, 3, 1, 11, 10, 0));

        mockMvc.perform(get("/stats")
                        .param("start", "2026-03-01 10:00:00")
                        .param("end", "2026-03-01 12:00:00")
                        .param("uris", "/events/1")
                        .param("unique", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hits").value(2));
    }

    @Test
    void getStatsShouldReturnBadRequestWhenStartAfterEnd() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "2026-03-01 12:00:00")
                        .param("end", "2026-03-01 10:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStatsShouldReturnBadRequestWhenDateFormatIsInvalid() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "2026-03-01T10:00:00")
                        .param("end", "2026-03-01 12:00:00"))
                .andExpect(status().isBadRequest());
    }

    private void createHit(String app, String uri, String ip, LocalDateTime timestamp) throws Exception {
        EndpointHit hit = new EndpointHit(null, app, uri, ip, timestamp);
        mockMvc.perform(post("/hit")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(hit)))
                .andExpect(status().isCreated());
    }
}
