package ru.practicum.explorewithme.service.request;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.explorewithme.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.explorewithme.dto.request.ParticipationRequestDto;
import ru.practicum.explorewithme.repository.request.RequestCountProjection;
import ru.practicum.explorewithme.exception.BadRequestException;
import ru.practicum.explorewithme.exception.ConflictDataException;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.mapper.RequestMapper;
import ru.practicum.explorewithme.model.event.Event;
import ru.practicum.explorewithme.model.event.State;
import ru.practicum.explorewithme.model.request.Request;
import ru.practicum.explorewithme.model.request.Status;
import ru.practicum.explorewithme.model.user.User;
import ru.practicum.explorewithme.repository.request.RequestRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private static final int ZERO_AVALIBLE_PARTICIPANT_SLOTS = 0;

    @PersistenceContext
    private EntityManager entityManager;

    private final RequestRepository requestRepository;

    @Override
    public Map<Long, Long> countRequestsByEventIds(Set<Long> eventIds) {
        log.info("Try to count request by event ids={}", eventIds);
        if (eventIds == null) {
            log.error("Try to get requests count by EventIds=null");
            throw new BadRequestException("Try to get requests count by EventIds=null");
        }
        if (eventIds.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, Long> map = getRequestsByEventIds(eventIds);
        log.info("Return request by event ids={}", eventIds);
        return map;
    }

    @Override
    public Long countRequestsByEventId(Long eventId) {
        log.info("Try to count request by event id={}", eventId);
        if (eventId == null) {
            log.error("Try to get request count by EventId=null");
            throw new BadRequestException("Try to get requests count by EventIds=null");
        }
        return requestRepository.countRequestsByEventId(eventId);
    }

    @Override
    public Map<Long, Long> countConfirmedRequestsByEventIds(Set<Long> eventIds) {
        log.info("Try to count confirmed request by event ids={}", eventIds);
        if (eventIds == null) {
            log.error("Try to get confirmed requests count by EventIds=null");
            throw new BadRequestException("Try to get confirmed requests count by EventIds=null");
        }
        if (eventIds.isEmpty()) {
            return new HashMap<>();
        }
        return getConfirmedRequestsByEventIds(eventIds);
    }

    @Override
    public Long countConfirmedRequestsByEventId(Long eventId) {
        log.info("Try to count confirmed request by event id={}", eventId);
        if (eventId == null) {
            log.error("Try to get confirmed request count by EventId=null");
            throw new BadRequestException("Try to get confirmed requests count by EventIds=null");
        }
        return requestRepository.countConfirmedRequestsByEventId(eventId);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Try to get User Requests by userId={}", userId);
        checkUserExistInDB(userId);
        List<Request> requests = requestRepository.findRequestsByRequesterId(userId);
        log.info("Requests found successfully by userId={}", userId);
        return requests.stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto addUserRequest(Long requesterId, Long eventId) {
        log.info("Try to make new Request");

        checkUserExistInDB(requesterId);
        checkEventExistInDB(eventId);
        checkRequestNotExistInDB(requesterId, eventId);

        Event eventProxy = entityManager.getReference(Event.class, eventId);
        User userProxy = entityManager.getReference(User.class, requesterId);

        checkRequesterIsNotOwnerEvent(requesterId, eventProxy);
        checkEventIsAbleToRequest(eventProxy);

        Status requestStatus = Boolean.FALSE.equals(eventProxy.getRequestModeration())
                || Objects.equals(eventProxy.getParticipantLimit(), 0)
                ? Status.CONFIRMED
                : Status.PENDING;

        LocalDateTime created = LocalDateTime.now();

        Request newRequest = new Request(null, created, eventProxy, userProxy, requestStatus);

        Request savedRequest = requestRepository.save(newRequest);
        ParticipationRequestDto requestDto = RequestMapper.toParticipationRequestDto(savedRequest);

        log.info("Save request={}", requestDto);

        return requestDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        Event event = getEventByOwnerOrThrow(userId, eventId);
        return requestRepository.findAllByEventId(event.getId()).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateEventRequests(Long userId,
                                                              Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        Event event = getEventByOwnerOrThrow(userId, eventId);
        if (updateRequest.getRequestIds() == null || updateRequest.getRequestIds().isEmpty()) {
            throw new BadRequestException("Request ids must not be empty");
        }
        if (updateRequest.getStatus() == null) {
            throw new BadRequestException("Status must not be null");
        }

        List<Request> requests = requestRepository.findAllByIdInAndEventId(updateRequest.getRequestIds(), eventId);
        if (requests.size() != updateRequest.getRequestIds().size()) {
            throw new NotFoundException("Request was not found");
        }
        if (requests.stream().anyMatch(request -> request.getStatus() != Status.PENDING)) {
            throw new ConflictDataException("Only pending requests can be updated");
        }

        if (updateRequest.getStatus() == Status.REJECTED) {
            requests.forEach(request -> request.setStatus(Status.REJECTED));
            List<Request> savedRequests = requestRepository.saveAll(requests);
            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(List.of())
                    .rejectedRequests(savedRequests.stream()
                            .map(RequestMapper::toParticipationRequestDto)
                            .toList())
                    .build();
        }

        if (updateRequest.getStatus() != Status.CONFIRMED) {
            throw new BadRequestException("Unsupported status=" + updateRequest.getStatus());
        }

        if (Boolean.FALSE.equals(event.getRequestModeration())
                || Objects.equals(event.getParticipantLimit(), ZERO_AVALIBLE_PARTICIPANT_SLOTS)) {
            throw new ConflictDataException("Confirmation is not required for this event");
        }

        long confirmedCount = countConfirmedRequestsByEventId(eventId);
        int availableSlots = event.getParticipantLimit() - (int) confirmedCount;
        if (availableSlots <= ZERO_AVALIBLE_PARTICIPANT_SLOTS) {
            throw new ConflictDataException("Event=" + eventId + " has no available spots for participation");
        }

        Map<Long, Integer> requestOrder = new HashMap<>();
        for (int i = 0; i < updateRequest.getRequestIds().size(); i++) {
            requestOrder.put(updateRequest.getRequestIds().get(i), i);
        }
        requests.sort(Comparator.comparingInt(request -> requestOrder.getOrDefault(request.getId(), Integer.MAX_VALUE)));

        List<Request> confirmedRequests = new ArrayList<>();
        List<Request> rejectedRequests = new ArrayList<>();

        for (Request request : requests) {
            if (availableSlots > ZERO_AVALIBLE_PARTICIPANT_SLOTS) {
                request.setStatus(Status.CONFIRMED);
                confirmedRequests.add(request);
                availableSlots--;
            } else {
                request.setStatus(Status.REJECTED);
                rejectedRequests.add(request);
            }
        }

        if (availableSlots == ZERO_AVALIBLE_PARTICIPANT_SLOTS) {
            Set<Long> handledRequestIds = requests.stream()
                    .map(Request::getId)
                    .collect(HashSet::new, HashSet::add, HashSet::addAll);
            List<Request> pendingRequests = requestRepository.findAllByEventIdAndStatus(eventId, Status.PENDING);
            pendingRequests.stream()
                    .filter(request -> !handledRequestIds.contains(request.getId()))
                    .forEach(request -> {
                        request.setStatus(Status.REJECTED);
                        rejectedRequests.add(request);
                    });
        }

        requestRepository.saveAll(confirmedRequests);
        requestRepository.saveAll(rejectedRequests);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests.stream()
                        .map(RequestMapper::toParticipationRequestDto)
                        .toList())
                .rejectedRequests(rejectedRequests.stream()
                        .map(RequestMapper::toParticipationRequestDto)
                        .toList())
                .build();
    }

    @Override
    @Transactional
    public ParticipationRequestDto rejectUserRequest(Long userId, Long requestId) {
        log.info("Try to reject requestId={} by userId={}", userId, requestId);
        checkUserExistInDB(userId);
        checkRequestExistInDB(requestId);
        Optional<Request> requestOptional = requestRepository.findById(requestId);
        Request request = requestOptional.get();
        if (!request.getRequester().getId().equals(userId)) {
            log.error("Canceled request can only requestor={}", request.getRequester().getId());
            throw new ConflictDataException("Canceled request can only requestor");
        }
        request.setStatus(Status.CANCELED);
        Request savedRequest = requestRepository.save(request);
        log.info("Successfully rejected requestId={} by userId={}", userId, requestId);
        return RequestMapper.toParticipationRequestDto(savedRequest);
    }

    private void checkRequestExistInDB(Long requestId) {
        if (!requestRepository.existsById(requestId)) {
            log.error("Request was not found with id={}", requestId);
            throw new NotFoundException("Request was not found with id=" + requestId);
        }
    }

    private void checkEventIsAbleToRequest(Event event) {
        if (!event.getState().equals(State.PUBLISHED)) {
            log.error("Event={} is not published", event.getId());
            throw new ConflictDataException("Event=" + event.getId() + " is not published");
        }
        if (event.getParticipantLimit() == null || Objects.equals(event.getParticipantLimit(), 0)) {
            return;
        }
        Long confirmedRequests = countConfirmedRequestsByEventId(event.getId());
        if (confirmedRequests >= event.getParticipantLimit()) {
            log.error("Event={} has no available spots for participation", event.getId());
            throw new ConflictDataException("Event=" + event.getId() + " has no available spots for participation");
        }

    }

    private void checkRequesterIsNotOwnerEvent(Long requesterId, Event eventProxy) {
        if (requesterId.equals(eventProxy.getInitiator().getId())) {
            log.error("Requester={} cant make request for its event", requesterId);
            throw new ConflictDataException("Requester cant make request for its event");
        }
    }

    private void checkRequestNotExistInDB(Long requesterId, Long eventId) {
        Optional<Request> requestOptional = requestRepository.findByRequesterIdAndEventId(requesterId, eventId);
        if (requestOptional.isPresent()) {
            log.error("Request with requesterId={} and eventId={} is already in DB", requesterId, eventId);
            throw new ConflictDataException("Request with requesterId and eventId is already in DB");
        }
    }

    private void checkUserExistInDB(Long userId) {
        if (!requestRepository.existsUserById(userId)) {
            log.error("User was not found with id={}", userId);
            throw new NotFoundException("User was not found with id=" + userId);
        }
    }

    private void checkEventExistInDB(Long eventId) {
        if (!requestRepository.existsEventById(eventId)) {
            log.error("Event was not found with id={}", eventId);
            throw new NotFoundException("Event was not found with id=" + eventId);
        }
    }

    private Map<Long, Long> getRequestsByEventIds(Set<Long> eventIds) {
        log.info("Try to getRequestsByEventIds={}", eventIds);
        List<RequestCountProjection> results = requestRepository.countRequestsByEventIds(eventIds);
        if (results.isEmpty()) {
            return new HashMap<>();
        }
        return results.stream()
                .collect(Collectors.toMap(
                        RequestCountProjection::getEventId,
                        RequestCountProjection::getConfirmedRequestsAmount
                ));
    }

    private Map<Long, Long> getConfirmedRequestsByEventIds(Set<Long> eventIds) {
        List<RequestCountProjection> results = requestRepository.countConfirmedRequestsByEventIds(eventIds);
        if (results.isEmpty()) {
            return new HashMap<>();
        }
        return results.stream()
                .collect(Collectors.toMap(
                        RequestCountProjection::getEventId,
                        RequestCountProjection::getConfirmedRequestsAmount
                ));
    }

    private Event getEventByOwnerOrThrow(Long userId, Long eventId) {
        checkUserExistInDB(userId);
        checkEventExistInDB(eventId);
        Event event = entityManager.getReference(Event.class, eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event was not found with id=" + eventId);
        }
        return event;
    }
}
