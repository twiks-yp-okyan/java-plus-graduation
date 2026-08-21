package ru.practicum.explorewithme.service.event;

import feign.FeignException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.client.AnalyzerClient;
import ru.practicum.explorewithme.client.CollectorClient;
import ru.practicum.explorewithme.dto.event.*;
import ru.practicum.explorewithme.dto.category.CategoryDto;
import ru.practicum.explorewithme.dto.user.UserDto;
import ru.practicum.explorewithme.exception.BadRequestException;
import ru.practicum.explorewithme.exception.ConflictDataException;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.feign.RequestClient;
import ru.practicum.explorewithme.feign.UserClient;
import ru.practicum.explorewithme.mapper.EventMapper;
import ru.practicum.explorewithme.mapper.LocationMapper;
import ru.practicum.explorewithme.model.category.Category;
import ru.practicum.explorewithme.model.event.Event;
import ru.practicum.explorewithme.model.event.State;
import ru.practicum.explorewithme.model.event.StateAction;
import ru.practicum.explorewithme.model.location.Location;
import ru.practicum.explorewithme.repository.EventRepository;
import ru.practicum.explorewithme.service.category.CategoryService;
import ru.yandex.practicum.grpc.message.RecommendedEventProto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final CategoryService categoryService;
    private final AnalyzerClient analyzerClient;
    private final CollectorClient collectorClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String uri = "/events/";
    private final String appName = "ewm-main-service";

    @Override
    public Page<EventFullDto> getEventByParam(EventAdminRequest eventAdminRequest, Pageable pageable) {
        log.info("Try to get event by param={}", eventAdminRequest);

        List<State> states = parseState(eventAdminRequest.states());
        LocalDateTime rangeStart = parseNullableDate(eventAdminRequest.rangeStart());
        LocalDateTime rangeEnd = parseNullableDate(eventAdminRequest.rangeEnd());
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("Range start must be before range end");
        }

        Specification<Event> specification = (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (eventAdminRequest.users() != null && !eventAdminRequest.users().isEmpty()) {
                predicates.add(root.get("initiatorId").in(eventAdminRequest.users()));
            }
            if (states != null && !states.isEmpty()) {
                predicates.add(root.get("state").in(states));
            }
            if (eventAdminRequest.categories() != null && !eventAdminRequest.categories().isEmpty()) {
                predicates.add(root.get("category").get("id").in(eventAdminRequest.categories()));
            }
            if (rangeStart != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
            }
            if (rangeEnd != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
            }

            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Event> page = eventRepository.findAll(specification, pageable);

        LocalDateTime earliestDate = getEarliestDateInPage(page);

        Set<Long> eventIds = getEventId(page);
        Map<Long, Integer> amountRequestsByEventIds = requestClient.getConfirmedRequestsCountByEventIds(eventIds);
        Map<Long, Double> eventsRating = getEventsRating(new ArrayList<>(eventIds));


        Page<EventFullDto> returnedPage = page.map(event -> {
            Integer requestsCount = amountRequestsByEventIds.get(event.getId());
            Integer amountRequestResult = requestsCount == null ? 0 : requestsCount;

            return EventMapper.toEventFullDto(
                    event,
                    getUserById(event.getInitiatorId()),
                    amountRequestResult,
                    eventsRating.getOrDefault(event.getId(), 0.0)
            );
        });

        log.info("Return event by param={}", returnedPage);
        return returnedPage;
    }

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        log.info("Try to create event by userId={}", userId);
        UserDto initiator = getUserById(userId);
        log.debug("Event Initiator = {}", initiator);
        CategoryDto categoryDto = categoryService.getCateGoryById(newEventDto.getCategory());
        checkParticipantLimit(newEventDto.getParticipantLimit());

        Event event = Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(new Category(categoryDto.id(), categoryDto.name()))
                .createdOn(LocalDateTime.now())
                .description(newEventDto.getDescription())
                .eventDate(parseDate(newEventDto.getEventDate()))
                .initiatorId(initiator.getId())
                .location(LocationMapper.mapToLocation(newEventDto.getLocation()))
                .paid(newEventDto.getPaid())
                .participantLimit(newEventDto.getParticipantLimit())
                .publishedOn(null)
                .requestModeration(newEventDto.getRequestModeration())
                .state(State.PENDING)
                .title(newEventDto.getTitle())
                .build();
        Event savedEvent = eventRepository.save(event);
        log.debug("Сохранен новый ивент с id = {}; автор - id = {}", savedEvent.getId(), savedEvent.getInitiatorId());
        return EventMapper.toEventFullDto(savedEvent, initiator, 0, 0.0);
    }

    @Override
    public EventFullDto getByUserIdAndId(Long userId, Long eventId) {
        Event event = getEventByUserIdAndIdOrThrow(userId, eventId);
        Integer confirmedRequests = requestClient.getConfirmedRequestsCountByEventId(eventId);
        Map<Long, Double> eventRating = getEventsRating(List.of(eventId));
        return EventMapper.toEventFullDto(event, getUserById(event.getInitiatorId()), confirmedRequests, eventRating.get(eventId));
    }

    @Override
    public List<EventShortDto> getByUserId(Long userId, int from, int size) {
        log.debug("Try to get List<EventShortDto> by userId={}, from={}, size={}", userId, from, size);
        UserDto user = getUserById(userId);
        PageRequest pageRequest = PageRequest.of(from > 0 ? from / size : 0, size);
        Page<Event> page = eventRepository.findAllByInitiatorId(user.getId(), pageRequest);
        if (page.isEmpty()) {
            return List.of();
        }

        LocalDateTime earliestDate = getEarliestDateInPage(page);
        Set<Long> eventIds = getEventId(page);
        Map<Long, Integer> amountRequestsByEventIds = requestClient.getConfirmedRequestsCountByEventIds(eventIds);
        Map<Long, Double> eventsRating = getEventsRating(new ArrayList<>(eventIds));

        return page.getContent().stream()
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        getUserById(event.getInitiatorId()),
                        amountRequestsByEventIds.getOrDefault(event.getId(), 0),
                        eventsRating.getOrDefault(event.getId(), 0.0)))
                .toList();
    }

    @Override
    public List<EventShortDto> searchPublicEvents(String text, List<Long> categories, Boolean paid,
                                                  String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                                  String sort, int from, int size, String requestUri, String ip) {
        log.info("Try to searchPublicEvents by param text={}, categories={}, paid={}", text, categories, paid);
        LocalDateTime start = parsePublicRangeStart(rangeStart, rangeEnd);
        LocalDateTime end = parseNullableDate(rangeEnd);

        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("Range start must be before range end");
        }

        log.debug("Поиск событий по фильтрам в БД");
        Specification<Event> specification = createSpecificationByParam(text, categories,
                paid, start, end);
        List<Event> eventsWithoutAvailableFilter = eventRepository.findAll(specification);
        log.debug("События по фльтрам поулчены. начало фильтрации по условию доступности");
        List<Event> events;
        if (onlyAvailable) {
            Set<Long> eventIds = eventsWithoutAvailableFilter.stream().map(Event::getId).collect(Collectors.toSet());
            Map<Long, Integer> eventsConfirmedRequestsCount = requestClient.getConfirmedRequestsCountByEventIds(eventIds);
            events = eventsWithoutAvailableFilter.stream()
                    .filter(
                            event -> isEventAvailable(event, eventsConfirmedRequestsCount.getOrDefault(event.getId(), 0))
                    )
                    .toList();
        } else {
            events = eventsWithoutAvailableFilter;
        }

        if (events.isEmpty()) {
            return List.of();
        }

        Set<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toSet());
        Map<Long, Integer> confirmedRequestsByEventIds = requestClient.getConfirmedRequestsCountByEventIds(eventIds);
        Map<Long, Double> eventsRating = getEventsRating(new ArrayList<>(eventIds));
        Comparator<Event> comparator = getPublicSortComparator(sort, eventsRating);

        List<EventShortDto> result = events.stream()
                .sorted(comparator)
                .skip(from)
                .limit(size)
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        getUserById(event.getInitiatorId()),
                        confirmedRequestsByEventIds.getOrDefault(event.getId(), 0),
                        eventsRating.getOrDefault(event.getId(), 0.0)))
                .toList();
        log.info("Return List<EventShortDto>.");
        log.info("Saved hit.");
        return result;
    }

    @Override
    public EventFullDto getPublishedEventById(Long userId, Long eventId, String requestUri, String ip) {
        Event event = eventRepository.findByIdAndState(eventId, State.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        log.debug("Запрос в request-service количества подствержденных заявок для события {}", eventId);
        Integer confirmedRequests = requestClient.getConfirmedRequestsCountByEventId(eventId);
        log.debug("Запрос в analyzer-service для рейтинга для события {}", eventId);
        Map<Long, Double> eventRating = getEventsRating(List.of(eventId));
        log.debug("Отправляем в collector действие VIEW для события {}", eventId);
        collectorClient.sendView(eventId, userId);
        log.debug("Маппинг в DTO и отправка в контроллер события {}", eventId);
        return EventMapper.toEventFullDto(event, getUserById(event.getInitiatorId()), confirmedRequests, eventRating.get(eventId));
    }

    @Override
    @Transactional
    public EventFullDto update(Long userId, Long eventId, UpdateEventUserRequest updateEvent) {
        Event event = getEventByUserIdAndIdOrThrow(userId, eventId);

        if (!(event.getState().equals(State.PENDING) || event.getState().equals(State.CANCELED))) {
            throw new ConflictDataException("Only pending or canceled events can be changed");
        }

        if (updateEvent.getAnnotation() != null && !updateEvent.getAnnotation().isEmpty()) {
            event.setAnnotation(updateEvent.getAnnotation());
        }
        if (updateEvent.getCategory() != null) {
            CategoryDto categoryDto = categoryService.getCateGoryById(updateEvent.getCategory());
            event.setCategory(new Category(categoryDto.id(), categoryDto.name()));
        }
        if (updateEvent.getDescription() != null && !updateEvent.getDescription().isEmpty()) {
            event.setDescription(updateEvent.getDescription());
        }
        if (updateEvent.getEventDate() != null && !updateEvent.getEventDate().isEmpty()) {
            event.setEventDate(parseDate(updateEvent.getEventDate()));
        }
        if (updateEvent.getLocation() != null) {
            event.setLocation(LocationMapper.mapToLocation(updateEvent.getLocation()));
        }
        if (updateEvent.getPaid() != null) {
            event.setPaid(updateEvent.getPaid());
        }
        if (updateEvent.getParticipantLimit() != null) {
            checkParticipantLimit(updateEvent.getParticipantLimit());
            event.setParticipantLimit(updateEvent.getParticipantLimit());
        }
        if (updateEvent.getRequestModeration() != null) {
            event.setRequestModeration(updateEvent.getRequestModeration());
        }
        if (updateEvent.getTitle() != null && !updateEvent.getTitle().isEmpty()) {
            event.setTitle(updateEvent.getTitle());
        }
        if (updateEvent.getStateAction() != null) {
            if (updateEvent.getStateAction().equals(StateAction.SEND_TO_REVIEW)) {
                event.setState(State.PENDING);
            } else if (updateEvent.getStateAction().equals(StateAction.CANCEL_REVIEW)) {
                event.setState(State.CANCELED);
            }
        }

        Map<Long, Double> eventRating = getEventsRating(List.of(eventId));

        Event savedEvent = eventRepository.save(event);
        Integer confirmedRequests = requestClient.getConfirmedRequestsCountByEventId(savedEvent.getId());
        return EventMapper.toEventFullDto(
                savedEvent,
                getUserById(savedEvent.getInitiatorId()),
                confirmedRequests,
                eventRating.get(eventId));
    }

    @Override
    @Transactional
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
        log.info("Try to update by admin event={}", eventId);
        Optional<Event> optionalEvent = eventRepository.findById(eventId);
        if (optionalEvent.isEmpty()) {
            log.error("Event not found by ID={}", eventId);
            throw new NotFoundException("Event not found by ID=" + eventId);
        }
        Event event = optionalEvent.get();
        Event updatedEvent = validateAndUpdate(event, updateEventAdminRequest);
        Event savedEvent = eventRepository.saveAndFlush(updatedEvent);

        Integer amountRequestsByEventId = requestClient.getConfirmedRequestsCountByEventId(savedEvent.getId());
        Map<Long, Double> eventRating = getEventsRating(List.of(eventId));

        return EventMapper.toEventFullDto(
                savedEvent,
                getUserById(savedEvent.getInitiatorId()),
                amountRequestsByEventId,
                eventRating.get(eventId));
    }

    @Override
    public EventFullDto getById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));
        Integer confirmedRequests = requestClient.getConfirmedRequestsCountByEventId(eventId);
        Map<Long, Double> eventRating = getEventsRating(List.of(eventId));
        return EventMapper.toEventFullDto(event, getUserById(event.getInitiatorId()), confirmedRequests, eventRating.get(eventId));
    }

    @Override
    public List<EventFullDto> getUserRecommendations(Long userId) {
        return analyzerClient.getRecommendationsForUser(userId, 20)
                .map(recommendedEvent -> this.getById(recommendedEvent.getEventId()))
                .toList();
    }

    @Override
    public void sendUserEventLike(Long userId, Long eventId) {
        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        EventFullDto eventFullDto = getById(eventId);
        if (!requestClient.checkUserRequestConfirmation(eventId, userId)) {
            throw new BadRequestException("Пользователь " + userId + " не зарегистрирован на мероприятие " + eventId);
        }
        if (LocalDateTime.parse(eventFullDto.getEventDate(), customFormatter).isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Пользователь " + userId + " не может лайкнуть непосещенное мероприятие " + eventId);
        }
        collectorClient.sendLike(eventId, userId);
    }

    private void checkParticipantLimit(Integer participantLimit) {
        if (participantLimit != null && participantLimit < 0) {
            log.error("ParticipantLimit can't be negative ={}", participantLimit);
            throw new BadRequestException("ParticipantLimit can't be negative");
        }
    }

    private Set<Long> getEventId(Page<Event> page) {
        log.info("Try to get getEventIds");
        Set<Long> set = page.getContent()
                .stream()
                .map(Event::getId)
                .collect(Collectors.toSet());
        log.info("Return Set<Long> EventIds");
        return set;
    }

    private LocalDateTime getEarliestDateInPage(Page<Event> page) {
        return page.getContent()
                .stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    private LocalDateTime parseDate(String stringDate) {
        try {
            return LocalDateTime.parse(stringDate, formatter);
        } catch (DateTimeParseException exception) {
            log.error("Not valid value stringDate={}", stringDate);
            throw new BadRequestException("Not valid value stringDate" + stringDate);
        }
    }

    private List<State> parseState(List<String> states) {
        if (states == null || states.isEmpty()) {
            return null;
        }
        try {
            return states.stream()
                    .map(State::valueOf)
                    .toList();
        } catch (IllegalArgumentException e) {
            log.error("Not valid value states={}", states);
            throw new BadRequestException("Not valid value states=" + states);
        }
    }

    private Event validateAndUpdate(Event event, UpdateEventAdminRequest request) {
        if (request.getStateAction() != null) {
            StateAction action = StateAction.valueOf(request.getStateAction());
            if (action.equals(StateAction.PUBLISH_EVENT)) {
                if (!event.getState().equals(State.PENDING)) {
                    log.info("Cannot publish the event because it's not in the right state={}", event.getState());
                    throw new ConflictDataException("Cannot publish the event because it's not in the right state");
                }
                event.setState(State.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (action.equals(StateAction.REJECT_EVENT)) {
                if (event.getState().equals(State.PUBLISHED)) {
                    throw new ConflictDataException("Cannot reject the published event");
                }
                event.setState(State.CANCELED);
            }
        }

        if (request.getEventDate() != null && !request.getEventDate().isEmpty()) {
            LocalDateTime eventDate = parseDate(request.getEventDate());
            if (eventDate.isBefore(LocalDateTime.now())) {
                log.error("event date is already in the past={}", eventDate);
                throw new BadRequestException("Event date must be in the future");
            }
            LocalDateTime minAllowedDate = event.getPublishedOn() == null
                    ? LocalDateTime.now().plusHours(1)
                    : event.getPublishedOn().plusHours(1);
            if (eventDate.isBefore(minAllowedDate)) {
                log.error("event date can't be earlier than={}", minAllowedDate);
                throw new ConflictDataException("event date can't be earlier than=" + minAllowedDate);
            }
            event.setEventDate(eventDate);
        }

        if (request.getAnnotation() != null && !request.getAnnotation().isEmpty()) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getCategory() != null) {
            CategoryDto categoryDto = categoryService.getCateGoryById(request.getCategory());
            event.setCategory(new Category(categoryDto.id(), categoryDto.name()));
        }

        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            event.setDescription(request.getDescription());
        }

        if (request.getLocation() != null) {
            event.setLocation(Location.builder()
                    .lat(request.getLocation().getLat())
                    .lon(request.getLocation().getLon())
                    .build());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            checkParticipantLimit(request.getParticipantLimit());
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getTitle() != null && !request.getTitle().isEmpty()) {
            event.setTitle(request.getTitle());
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

    private Event getEventByUserIdAndIdOrThrow(Long userId, Long eventId) {
        UserDto user = getUserById(userId);
        return eventRepository.findByIdAndInitiatorId(eventId, user.getId())
                .orElseThrow(() -> new NotFoundException("Event not found by ID=" + eventId));
    }

    private LocalDateTime parseNullableDate(String stringDate) {
        if (stringDate == null || stringDate.isBlank()) {
            return null;
        }
        return parseDate(stringDate);
    }

    private LocalDateTime parsePublicRangeStart(String rangeStart, String rangeEnd) {
        if ((rangeStart == null || rangeStart.isBlank()) && (rangeEnd == null || rangeEnd.isBlank())) {
            return LocalDateTime.now();
        }
        return parseNullableDate(rangeStart);
    }

    private LocalDateTime getEarliestDate(List<Event> events) {
        return events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    private Comparator<Event> getPublicSortComparator(String sort, Map<Long, Double> eventsRating) {
        if (sort == null || sort.isBlank() || sort.equals("EVENT_DATE")) {
            return Comparator.comparing(Event::getEventDate);
        }
        if (sort.equals("VIEWS")) {
            return Comparator.comparingDouble((Event event) -> eventsRating.getOrDefault(event.getId(), 0.0))
                    .reversed();
        }
        throw new BadRequestException("Unknown sort option=" + sort);
    }

    private Specification<Event> createSpecificationByParam(String text, List<Long> categories,
                                                            Boolean paid, LocalDateTime start,
                                                            LocalDateTime end) {

        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(root.get("state"), State.PUBLISHED));

            if (text != null && !text.trim().isEmpty()) {
                String searchPattern = "%" + text.toLowerCase() + "%";
                Predicate annotationMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("annotation")), searchPattern);
                Predicate descriptionMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")), searchPattern);
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.or(annotationMatch, descriptionMatch));
            }

            if (categories != null && !categories.isEmpty()) {
                predicate = criteriaBuilder.and(predicate,
                        root.get("category").get("id").in(categories));
            }

            if (paid != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("paid"), paid));
            }

            if (start != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), start));
            }

            if (end != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.lessThanOrEqualTo(root.get("eventDate"), end));
            }

            return predicate;
        };
    }

    private boolean isEventAvailable(Event event, Integer confirmedRequestsCount) {
        Integer participantLimit = event.getParticipantLimit();
        if (participantLimit == null || participantLimit == 0) return true;
        return participantLimit > confirmedRequestsCount;
    }

    private Map<Long, Double> getEventsRating(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return analyzerClient.getInteractionsCount(eventIds)
                .collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore));
    }

}
