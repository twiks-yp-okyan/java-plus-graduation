package ru.practicum.explorewithme.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.explorewithme.dto.event.EventFullDto;
import ru.practicum.explorewithme.dto.event.EventShortDto;
import ru.practicum.explorewithme.dto.user.UserDto;
import ru.practicum.explorewithme.model.event.Event;

import java.time.format.DateTimeFormatter;

@UtilityClass
public class EventMapper {
    private final DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EventFullDto toEventFullDto(Event event, UserDto initiatorDto, Integer confirmedRequests, Double rating) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .createdOn(event.getCreatedOn().format(customFormatter))
                .description(event.getDescription())
                .eventDate(event.getEventDate().format(customFormatter))
                .initiator(UserMapper.toUserShortDto(initiatorDto))
                .location(LocationMapper.copy(event.getLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn() == null ? null : event.getPublishedOn().format(customFormatter))
                .requestModeration(event.getRequestModeration())
                .state(event.getState().name())
                .title(event.getTitle())
                .rating(rating)
                .build();
    }

    public EventShortDto toEventShortDto(Event event, UserDto initiatorDto, Integer confirmedRequests, Double rating) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .eventDate(event.getEventDate().format(customFormatter))
                .initiator(UserMapper.toUserShortDto(initiatorDto))
                .paid(event.getPaid())
                .title(event.getTitle())
                .rating(rating)
                .build();
    }
}
