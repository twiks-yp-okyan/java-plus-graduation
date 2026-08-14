package ru.practicum.explorewithme.dto.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import ru.practicum.explorewithme.dto.category.CategoryDto;
import ru.practicum.explorewithme.dto.user.UserShortDto;
import ru.practicum.explorewithme.model.location.Location;

import static org.junit.jupiter.api.Assertions.assertTrue;

@JsonTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class EventFullDtoTest {
    private final ObjectMapper mapper;

    @Test
    void testEventFullDtoSerialization() throws Exception {
        EventFullDto dto = EventFullDto.builder()
                .id(1L)
                .annotation("test_annotation")
                .category(new CategoryDto(10L, "category"))
                .confirmedRequests(100L)
                .createdOn("2022-09-06 11:00:23")
                .description("test_description")
                .eventDate("2026-03-11 20:26:29")
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

        String json = mapper.writeValueAsString(dto);

        assertTrue(json.contains("\"annotation\":\"test_annotation\""));
        assertTrue(json.contains("\"id\":1"));
    }

}