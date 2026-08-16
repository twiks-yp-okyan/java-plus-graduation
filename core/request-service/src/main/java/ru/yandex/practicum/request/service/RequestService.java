package ru.yandex.practicum.request.service;

import ru.yandex.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.yandex.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.yandex.practicum.request.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RequestService {
    Map<Long, Long> countRequestsByEventIds(Set<Long> eventIds);

    Integer countRequestsByEventId(Long eventId);

    Map<Long, Long> countConfirmedRequestsByEventIds(Set<Long> eventIds);

    Integer countConfirmedRequestsByEventId(Long eventId);

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto addUserRequest(Long userId, Long eventId);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateEventRequests(Long userId,
                                                       Long eventId,
                                                       EventRequestStatusUpdateRequest updateRequest);

    ParticipationRequestDto rejectUserRequest(Long userId, Long requestId);

    boolean checkUserRequestConfirmation(Long eventId, Long userId);
}
