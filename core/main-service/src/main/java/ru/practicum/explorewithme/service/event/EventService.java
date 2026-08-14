package ru.practicum.explorewithme.service.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.practicum.explorewithme.dto.event.*;

import java.util.List;

public interface EventService {
    Page<EventFullDto> getEventByParam(EventAdminRequest eventAdminRequest, Pageable pageable);

    EventFullDto create(Long userId, NewEventDto newEventDto);

    EventFullDto getByUserIdAndId(Long userId, Long eventId);

    List<EventShortDto> getByUserId(Long userId, int from, int size);

    List<EventShortDto> searchPublicEvents(String text,
                                           List<Long> categories,
                                           Boolean paid,
                                           String rangeStart,
                                           String rangeEnd,
                                           Boolean onlyAvailable,
                                           String sort,
                                           int from,
                                           int size,
                                           String requestUri,
                                           String ip);

    EventFullDto getPublishedEventById(Long eventId, String requestUri, String ip);

    EventFullDto update(Long userId, Long eventId, UpdateEventUserRequest updateEvent);

    EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest);
}
