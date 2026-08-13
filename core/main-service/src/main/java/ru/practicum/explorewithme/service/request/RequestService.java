package ru.practicum.explorewithme.service.request;

import ru.practicum.explorewithme.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.explorewithme.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.explorewithme.dto.request.ParticipationRequestDto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RequestService {
    Map<Long, Long> countRequestsByEventIds(Set<Long> eventIds);

    Long countRequestsByEventId(Long eventId);

    Map<Long, Long> countConfirmedRequestsByEventIds(Set<Long> eventIds);

    Long countConfirmedRequestsByEventId(Long eventId);

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto addUserRequest(Long userId, Long eventId);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateEventRequests(Long userId,
                                                       Long eventId,
                                                       EventRequestStatusUpdateRequest updateRequest);

    ParticipationRequestDto rejectUserRequest(Long userId, Long requestId);
}
