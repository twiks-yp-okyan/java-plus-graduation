package ru.practicum.explorewithme.controller.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.client.StatClient;
import ru.practicum.explorewithme.dto.EndpointHit;
import ru.practicum.explorewithme.dto.ViewStats;
import ru.practicum.explorewithme.dto.event.UpdateEventAdminRequest;
import ru.practicum.explorewithme.model.category.Category;
import ru.practicum.explorewithme.model.event.Event;
import ru.practicum.explorewithme.model.event.State;
import ru.practicum.explorewithme.model.location.Location;
import ru.practicum.explorewithme.model.user.User;
import ru.practicum.explorewithme.repository.CategoryRepository;
import ru.practicum.explorewithme.repository.EventRepository;
import ru.practicum.explorewithme.repository.LocationRepository;
import ru.practicum.explorewithme.repository.request.RequestRepository;
import ru.practicum.explorewithme.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventAdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RequestRepository requestRepository;

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();
        eventRepository.deleteAll();
        locationRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void updateEventShouldReturnBadRequestWhenEventDateAlreadyPast() throws Exception {
        User owner = userRepository.save(User.builder()
                .name("owner")
                .email("owner@example.com")
                .build());
        Category category = categoryRepository.save(Category.builder()
                .name("category")
                .build());
        Location location = Location.builder()
                .lat(10.0f)
                .lon(20.0f)
                .build();
        Event event = eventRepository.save(Event.builder()
                .annotation("A".repeat(30))
                .category(category)
                .createdOn(LocalDateTime.now().minusDays(1))
                .description("D".repeat(40))
                .eventDate(LocalDateTime.now().plusDays(10))
                .initiator(owner)
                .location(location)
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(State.PENDING)
                .title("event title")
                .build());

        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .eventDate("2000-01-01 00:00:00")
                .location(Location.builder().lat(10.0f).lon(20.0f).build())
                .build();

        mockMvc.perform(patch("/admin/events/{eventId}", event.getId())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class StubStatClientConfig {
        @Bean
        @Primary
        StatClient statClient() {
            return new StatClient(new org.springframework.web.client.RestTemplate()) {
                @Override
                public void saveHit(EndpointHit hitDto) {
                }

                @Override
                public List<ViewStats> getStat(LocalDateTime start,
                                               LocalDateTime end,
                                               List<String> uris,
                                               boolean unique) {
                    return List.of();
                }
            };
        }
    }
}
