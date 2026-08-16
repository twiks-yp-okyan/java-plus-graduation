package ru.yandex.practicum.request.service;

import feign.FeignException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.yandex.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.yandex.practicum.request.dto.ParticipationRequestDto;
import ru.yandex.practicum.request.dto.event.EventFullDto;
import ru.yandex.practicum.request.dto.event.State;
import ru.yandex.practicum.request.dto.user.UserDto;
import ru.yandex.practicum.request.exception.BadRequestException;
import ru.yandex.practicum.request.exception.ConflictDataException;
import ru.yandex.practicum.request.exception.NotFoundException;
import ru.yandex.practicum.request.feign.EventClient;
import ru.yandex.practicum.request.feign.UserClient;
import ru.yandex.practicum.request.model.Request;
import ru.yandex.practicum.request.model.RequestMapper;
import ru.yandex.practicum.request.model.Status;
import ru.yandex.practicum.request.repository.RequestCountProjection;
import ru.yandex.practicum.request.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private static final int ZERO_AVAILABLE_PARTICIPANT_SLOTS = 0;

    @PersistenceContext
    private EntityManager entityManager;

    private final RequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    public Map<Long, Long> countRequestsByEventIds(Set<Long> eventIds) {
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

    @Override
    public Integer countRequestsByEventId(Long eventId) {
        if (eventId == null) {
            log.error("Try to get request count by EventId=null");
            throw new BadRequestException("Try to get requests count by EventIds=null");
        }
        log.info("Try to count request by event id={}", eventId);
        return requestRepository.countRequestsByEventId(eventId);
    }

    @Override
    public Map<Long, Long> countConfirmedRequestsByEventIds(Set<Long> eventIds) {
        log.info("Try to count confirmed request by event ids={}", eventIds);
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

    @Override
    public Integer countConfirmedRequestsByEventId(Long eventId) {
        log.info("Try to count confirmed request by event id={}", eventId);
        return requestRepository.countConfirmedRequestsByEventId(eventId);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Try to get User Requests by userId={}", userId);
        UserDto user = getUserById(userId);
        List<Request> requests = requestRepository.findRequestsByRequesterId(user.getId());
        log.info("Requests found successfully by userId={}", user.getId());
        return requests.stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto addUserRequest(Long requesterId, Long eventId) {
        log.info("Создание запроса на участие в событии id = {} от пользователя с id = {}", eventId, requesterId);

        UserDto user = getUserById(requesterId);
        EventFullDto event = getEventById(eventId);
        checkRequestNotExistInDB(user.getId(), eventId);


        checkRequesterIsNotOwnerEvent(user.getId(), event);
        checkEventIsAbleToRequest(event);

        Status requestStatus = Boolean.FALSE.equals(event.getRequestModeration())
                || Objects.equals(event.getParticipantLimit(), 0)
                ? Status.CONFIRMED
                : Status.PENDING;

        LocalDateTime created = LocalDateTime.now();

        Request newRequest = new Request(null, created, eventId, user.getId(), requestStatus);

        Request savedRequest = requestRepository.save(newRequest);
        ParticipationRequestDto requestDto = RequestMapper.toParticipationRequestDto(savedRequest);

        log.info("Save request={}", requestDto);

        return requestDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        EventFullDto event = getEventByOwnerOrThrow(userId, eventId);
        return requestRepository.findAllByEventId(event.getId()).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateEventRequests(Long userId,
                                                              Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        EventFullDto event = getEventByOwnerOrThrow(userId, eventId);
        List<Request> requests = requestRepository.findAllByIdInAndEventId(updateRequest.getRequestIds(), eventId);
        if (requests.size() != updateRequest.getRequestIds().size()) {
            throw new NotFoundException("Not all requests was not found by provided ids");
        }
        if (requests.stream().anyMatch(request -> request.getStatus() != Status.PENDING)) {
            throw new ConflictDataException("Only pending requests can be updated");
        }
        if (Boolean.FALSE.equals(event.getRequestModeration())
                || Objects.equals(event.getParticipantLimit(), ZERO_AVAILABLE_PARTICIPANT_SLOTS)) {
            throw new ConflictDataException("Confirmation is not required for this event");
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
        // for CONFIRMED
        Integer confirmedCount = countConfirmedRequestsByEventId(eventId);
        int availableSlots = event.getParticipantLimit() - confirmedCount;
        if (availableSlots <= ZERO_AVAILABLE_PARTICIPANT_SLOTS) {
            throw new ConflictDataException("Event=" + eventId + " has no available slots for participation");
        }
        // чтобы отменить заявки, не попавшие из-за "SOLD OUT" в хронологическом порядке (хз, почему не на уровне БД сделали коллеги)
        Map<Long, Integer> requestOrder = new HashMap<>();
        for (int i = 0; i < updateRequest.getRequestIds().size(); i++) {
            requestOrder.put(updateRequest.getRequestIds().get(i), i);
        }
        requests.sort(Comparator.comparingInt(request -> requestOrder.getOrDefault(request.getId(), Integer.MAX_VALUE)));

        List<Request> confirmedRequests = new ArrayList<>();
        List<Request> rejectedRequests = new ArrayList<>();

        for (Request request : requests) {
            if (availableSlots > ZERO_AVAILABLE_PARTICIPANT_SLOTS) {
                request.setStatus(Status.CONFIRMED);
                confirmedRequests.add(request);
                availableSlots--;
            } else {
                request.setStatus(Status.REJECTED);
                rejectedRequests.add(request);
            }
        }
        // при заполнении всей брони отменить все оставшиеся заявки на мероприятие, которых не было в запросе (надо ли?)
        if (availableSlots == ZERO_AVAILABLE_PARTICIPANT_SLOTS) {
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
        UserDto user = getUserById(userId);
        checkRequestExistInDB(requestId);

        Request request = requestRepository.findById(requestId)
                .orElseThrow(
                        () -> new NotFoundException("Запрос на участие не найден по id = " + requestId)
                );
        if (!request.getRequesterId().equals(user.getId())) {
            log.error("Cancel request can only its requestor = {}", request.getRequesterId());
            throw new ConflictDataException("Canceled request can only requestor");
        }
        request.setStatus(Status.CANCELED);
        request = requestRepository.save(request);
        log.info("Successfully rejected requestId={} by userId={}", requestId, user.getId());
        return RequestMapper.toParticipationRequestDto(request);
    }

    @Override
    public boolean checkUserRequestConfirmation(Long eventId, Long userId) {
        return requestRepository.findByRequesterIdAndEventId(userId, eventId)
                .map(request -> request.getStatus().equals(Status.CONFIRMED))
                .orElseThrow(
                        () -> new NotFoundException("Couldn't find request from requestorId="
                                + userId + " to eventId=" + eventId)
                );
    }

    private void checkRequestExistInDB(Long requestId) {
        if (!requestRepository.existsById(requestId)) {
            log.error("Request was not found with id={}", requestId);
            throw new NotFoundException("Request was not found with id=" + requestId);
        }
    }

    private void checkEventIsAbleToRequest(EventFullDto event) {
        if (!event.getState().equals(State.PUBLISHED.name())) {
            log.error("Event={} is not published", event.getId());
            throw new ConflictDataException("Event=" + event.getId() + " is not published");
        }
        if (event.getParticipantLimit() == null || Objects.equals(event.getParticipantLimit(), 0)) {
            return;
        }
        Integer confirmedRequests = countConfirmedRequestsByEventId(event.getId());
        if (confirmedRequests >= event.getParticipantLimit()) {
            log.error("Event={} has no available spots for participation", event.getId());
            throw new ConflictDataException("Event=" + event.getId() + " has no available spots for participation");
        }

    }

    private void checkRequesterIsNotOwnerEvent(Long requesterId, EventFullDto eventProxy) {
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

    private EventFullDto getEventByOwnerOrThrow(Long userId, Long eventId) {
        UserDto user = getUserById(userId);
        EventFullDto event = getEventById(eventId);
        if (!event.getInitiator().getId().equals(user.getId())) {
            throw new BadRequestException(
                    String.format("Для события id = %d пользователь с id = %d не является инициатором.", eventId, userId)
            );
        }
        return event;
    }

    private UserDto getUserById(Long userId) {
        try {
            log.debug("Попытка получить пользователя из user-service по id = {}", userId);
            return userClient.getById(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("User not found by ID=" + userId);
        }
    }

    private EventFullDto getEventById(Long eventId) {
        try {
            log.debug("Попытка получить событие из event-service по его id = {}", eventId);
            return eventClient.getById(eventId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Event was not found with id=" + eventId);
        }
    }
}
