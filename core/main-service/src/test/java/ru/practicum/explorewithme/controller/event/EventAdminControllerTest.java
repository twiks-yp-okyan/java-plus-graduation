package ru.practicum.explorewithme.controller.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.dto.category.CategoryDto;
import ru.practicum.explorewithme.dto.event.EventAdminRequest;
import ru.practicum.explorewithme.dto.event.EventFullDto;
import ru.practicum.explorewithme.dto.event.UpdateEventAdminRequest;
import ru.practicum.explorewithme.dto.user.UserShortDto;
import ru.practicum.explorewithme.model.location.Location;
import ru.practicum.explorewithme.service.event.EventService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventAdminController.class)
class EventAdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private EventService eventService;

    @Test
    void getEventsShouldReturnEventsCorrectly() throws Exception {
        List<Long> users = List.of(1L, 2L);
        List<String> states = List.of("PUBLISHED", "PENDING");
        List<Long> categories = List.of(10L, 20L);
        String rangeStart = "2023-01-01 00:00:00";
        String rangeEnd = "2023-12-31 23:59:59";
        Integer from = 30;
        Integer size = 10;

        EventAdminRequest expectedRequest = new EventAdminRequest(
                users, states, categories, rangeStart, rangeEnd
        );

        EventFullDto expectedEvent = EventFullDto.builder()
                .annotation("test")
                .category(new CategoryDto(10L, "category"))
                .confirmedRequests(100L)
                .createdOn("2022-09-06 11:00:23")
                .description("description_test")
                .eventDate("2024-12-31 15:10:05")
                .id(1L)
                .initiator(new UserShortDto(2L, "user_test"))
                .location(new Location(10L, 55.754167F, 37.62F))
                .paid(true)
                .participantLimit(10)
                .publishedOn("2022-09-06 15:10:05")
                .requestModeration(true)
                .state("PUBLISHED")
                .title("test_title")
                .views(999L)
                .build();

        List<EventFullDto> content = List.of(expectedEvent);
        Pageable expectedPageable = PageRequest.of(from / size, size);
        Page<EventFullDto> expectedPage = new PageImpl<>(content, expectedPageable, 1L);

        when(eventService.getEventByParam(expectedRequest, expectedPageable))
                .thenReturn(expectedPage);


        mockMvc.perform(get("/admin/events")
                        .param("users", "1", "2")
                        .param("states", "PUBLISHED", "PENDING")
                        .param("categories", "10", "20")
                        .param("rangeStart", rangeStart)
                        .param("rangeEnd", rangeEnd)
                        .param("from", from.toString())
                        .param("size", size.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].annotation").value("test"))
                .andExpect(jsonPath("$[0].category.id").value(10))
                .andExpect(jsonPath("$[0].category.name").value("category"))
                .andExpect(jsonPath("$[0].confirmedRequests").value(100))
                .andExpect(jsonPath("$[0].createdOn").value("2022-09-06 11:00:23"))
                .andExpect(jsonPath("$[0].description").value("description_test"))
                .andExpect(jsonPath("$[0].eventDate").value("2024-12-31 15:10:05"))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].initiator.id").value(2))
                .andExpect(jsonPath("$[0].initiator.name").value("user_test"))
                .andExpect(jsonPath("$[0].location.id").value(10))
                .andExpect(jsonPath("$[0].location.lat").value(55.754166))
                .andExpect(jsonPath("$[0].location.lon").value(37.62))
                .andExpect(jsonPath("$[0].paid").value(true))
                .andExpect(jsonPath("$[0].participantLimit").value(10))
                .andExpect(jsonPath("$[0].publishedOn").value("2022-09-06 15:10:05"))
                .andExpect(jsonPath("$[0].requestModeration").value(true))
                .andExpect(jsonPath("$[0].state").value("PUBLISHED"))
                .andExpect(jsonPath("$[0].title").value("test_title"))
                .andExpect(jsonPath("$[0].views").value(999));

        verify(eventService, times(1)).getEventByParam(eq(expectedRequest), eq(expectedPageable));
    }

    @Test
    void getEventsShouldReturnEventsWithDefaultParamCorrectly() throws Exception {
        List<Long> users = List.of(1L, 2L);
        List<String> states = List.of("PUBLISHED", "PENDING");
        List<Long> categories = List.of(10L, 20L);
        String rangeStart = "2023-01-01 00:00:00";
        String rangeEnd = "2023-12-31 23:59:59";

        EventAdminRequest expectedRequest = new EventAdminRequest(
                users, states, categories, rangeStart, rangeEnd
        );

        EventFullDto expectedEvent = EventFullDto.builder()
                .annotation("test")
                .category(new CategoryDto(10L, "category"))
                .confirmedRequests(100L)
                .createdOn("2022-09-06 11:00:23")
                .description("description_test")
                .eventDate("2024-12-31 15:10:05")
                .id(1L)
                .initiator(new UserShortDto(2L, "user_test"))
                .location(new Location(10L, 55.754167F, 37.62F))
                .paid(true)
                .participantLimit(10)
                .publishedOn("2022-09-06 15:10:05")
                .requestModeration(true)
                .state("PUBLISHED")
                .title("test_title")
                .views(999L)
                .build();

        List<EventFullDto> content = List.of(expectedEvent);
        Pageable expectedPageable = PageRequest.of(0 / 10, 10);
        Page<EventFullDto> expectedPage = new PageImpl<>(content, expectedPageable, 1L);

        when(eventService.getEventByParam(expectedRequest, expectedPageable))
                .thenReturn(expectedPage);


        mockMvc.perform(get("/admin/events")
                        .param("users", "1", "2")
                        .param("states", "PUBLISHED", "PENDING")
                        .param("categories", "10", "20")
                        .param("rangeStart", rangeStart)
                        .param("rangeEnd", rangeEnd))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].annotation").value("test"))
                .andExpect(jsonPath("$[0].category.id").value(10))
                .andExpect(jsonPath("$[0].category.name").value("category"))
                .andExpect(jsonPath("$[0].confirmedRequests").value(100))
                .andExpect(jsonPath("$[0].createdOn").value("2022-09-06 11:00:23"))
                .andExpect(jsonPath("$[0].description").value("description_test"))
                .andExpect(jsonPath("$[0].eventDate").value("2024-12-31 15:10:05"))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].initiator.id").value(2))
                .andExpect(jsonPath("$[0].initiator.name").value("user_test"))
                .andExpect(jsonPath("$[0].location.id").value(10))
                .andExpect(jsonPath("$[0].location.lat").value(55.754166))
                .andExpect(jsonPath("$[0].location.lon").value(37.62))
                .andExpect(jsonPath("$[0].paid").value(true))
                .andExpect(jsonPath("$[0].participantLimit").value(10))
                .andExpect(jsonPath("$[0].publishedOn").value("2022-09-06 15:10:05"))
                .andExpect(jsonPath("$[0].requestModeration").value(true))
                .andExpect(jsonPath("$[0].state").value("PUBLISHED"))
                .andExpect(jsonPath("$[0].title").value("test_title"))
                .andExpect(jsonPath("$[0].views").value(999));

        verify(eventService, times(1)).getEventByParam(eq(expectedRequest), eq(expectedPageable));
    }

    @Test
    void updateEventShouldUpdateFullEventCorrectly() throws Exception {
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .annotation("test_annotation".repeat(15))
                .category(10L)
                .description("test_description".repeat(20))
                .eventDate("2026-03-11 20:26:29")
                .location(new Location(30L, 1.1f, 2.2f))
                .paid(true)
                .participantLimit(40)
                .requestModeration(true)
                .stateAction("PUBLISH_EVENT")
                .title("test_title")
                .build();


        EventFullDto expectedEvent = EventFullDto.builder()
                .annotation("test_annotation".repeat(15))
                .category(new CategoryDto(10L, "category"))
                .confirmedRequests(100L)
                .createdOn("2022-09-06 11:00:23")
                .description("test_description".repeat(20))
                .eventDate("2026-03-11 20:26:29")
                .id(1L)
                .initiator(new UserShortDto(2L, "user_test"))
                .location(new Location(30L, 1.1f, 2.2f))
                .paid(true)
                .participantLimit(40)
                .publishedOn("2022-09-06 15:10:05")
                .requestModeration(true)
                .state("PUBLISHED")
                .title("test_title")
                .views(999L)
                .build();

        String content = mapper.writeValueAsString(request);


        when(eventService.updateEventAdmin(1L, request)).thenReturn(expectedEvent);


        mockMvc.perform(patch("/admin/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.annotation").value("test_annotation".repeat(15)))
                .andExpect(jsonPath("$.category.id").value(10))
                .andExpect(jsonPath("$.category.name").value("category"))
                .andExpect(jsonPath("$.confirmedRequests").value(100))
                .andExpect(jsonPath("$.createdOn").value("2022-09-06 11:00:23"))
                .andExpect(jsonPath("$.description").value("test_description".repeat(20)))
                .andExpect(jsonPath("$.eventDate").value("2026-03-11 20:26:29"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.initiator.id").value(2))
                .andExpect(jsonPath("$.initiator.name").value("user_test"))
                .andExpect(jsonPath("$.location.id").value(30))
                .andExpect(jsonPath("$.location.lat").value(1.1))
                .andExpect(jsonPath("$.location.lon").value(2.2))
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.participantLimit").value(40))
                .andExpect(jsonPath("$.publishedOn").value("2022-09-06 15:10:05"))
                .andExpect(jsonPath("$.requestModeration").value(true))
                .andExpect(jsonPath("$.state").value("PUBLISHED"))
                .andExpect(jsonPath("$.title").value("test_title"))
                .andExpect(jsonPath("$.views").value(999));

        verify(eventService, times(1)).updateEventAdmin(eq(1L), eq(request));
    }

    @Test
    void updateEventShouldUpdatePartlyEventCorrectly() throws Exception {
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .annotation("test_annotation".repeat(15))
                .description("test_description".repeat(20))
                .eventDate("2026-03-11 20:26:29")
                .participantLimit(40)
                .requestModeration(true)
                .stateAction("PUBLISH_EVENT")
                .build();


        EventFullDto expectedEvent = EventFullDto.builder()
                .annotation("test_annotation".repeat(15))
                .category(new CategoryDto(10L, "category"))
                .confirmedRequests(100L)
                .createdOn("2022-09-06 11:00:23")
                .description("test_description".repeat(20))
                .eventDate("2026-03-11 20:26:29")
                .id(1L)
                .initiator(new UserShortDto(2L, "user_test"))
                .location(new Location(30L, 1.1f, 2.2f))
                .paid(true)
                .participantLimit(40)
                .publishedOn("2022-09-06 15:10:05")
                .requestModeration(true)
                .state("PUBLISHED")
                .title("test_title")
                .views(999L)
                .build();

        String content = mapper.writeValueAsString(request);


        when(eventService.updateEventAdmin(1L, request)).thenReturn(expectedEvent);


        mockMvc.perform(patch("/admin/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.annotation").value("test_annotation".repeat(15)))
                .andExpect(jsonPath("$.category.id").value(10))
                .andExpect(jsonPath("$.category.name").value("category"))
                .andExpect(jsonPath("$.confirmedRequests").value(100))
                .andExpect(jsonPath("$.createdOn").value("2022-09-06 11:00:23"))
                .andExpect(jsonPath("$.description").value("test_description".repeat(20)))
                .andExpect(jsonPath("$.eventDate").value("2026-03-11 20:26:29"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.initiator.id").value(2))
                .andExpect(jsonPath("$.initiator.name").value("user_test"))
                .andExpect(jsonPath("$.location.id").value(30))
                .andExpect(jsonPath("$.location.lat").value(1.1))
                .andExpect(jsonPath("$.location.lon").value(2.2))
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.participantLimit").value(40))
                .andExpect(jsonPath("$.publishedOn").value("2022-09-06 15:10:05"))
                .andExpect(jsonPath("$.requestModeration").value(true))
                .andExpect(jsonPath("$.state").value("PUBLISHED"))
                .andExpect(jsonPath("$.title").value("test_title"))
                .andExpect(jsonPath("$.views").value(999));

        verify(eventService, times(1)).updateEventAdmin(eq(1L), eq(request));
    }

    @Test
    void updateEventShouldThrowExceptionWhenAnnotationIncorrect() throws Exception {
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .annotation("test")
                .category(10L)
                .description("test_description".repeat(20))
                .eventDate("2026-03-11 20:26:29")
                .location(new Location(30L, 1.1f, 2.2f))
                .paid(true)
                .participantLimit(40)
                .requestModeration(true)
                .stateAction("PUBLISH_EVENT")
                .title("test_title")
                .build();


        String content = mapper.writeValueAsString(request);

        mockMvc.perform(patch("/admin/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(eventService, times(0)).updateEventAdmin(eq(1L), eq(request));
    }

    @Test
    void updateEventShouldThrowExceptionWhenDescriptionIncorrect() throws Exception {
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .annotation("test_annotation".repeat(10))
                .category(10L)
                .description("test")
                .eventDate("2026-03-11 20:26:29")
                .location(new Location(30L, 1.1f, 2.2f))
                .paid(true)
                .participantLimit(40)
                .requestModeration(true)
                .stateAction("PUBLISH_EVENT")
                .title("test_title")
                .build();


        String content = mapper.writeValueAsString(request);

        mockMvc.perform(patch("/admin/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(eventService, times(0)).updateEventAdmin(eq(1L), eq(request));
    }

    @Test
    void updateEventShouldThrowExceptionWhenTitleIncorrect() throws Exception {
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .annotation("test_annotation".repeat(10))
                .category(10L)
                .description("test_description".repeat(10))
                .eventDate("2026-03-11 20:26:29")
                .location(new Location(30L, 1.1f, 2.2f))
                .paid(true)
                .participantLimit(40)
                .requestModeration(true)
                .stateAction("PUBLISH_EVENT")
                .title("test_title".repeat(100))
                .build();


        String content = mapper.writeValueAsString(request);

        mockMvc.perform(patch("/admin/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(eventService, times(0)).updateEventAdmin(eq(1L), eq(request));
    }
}
