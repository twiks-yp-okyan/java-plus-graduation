package ru.practicum.explorewithme.service.event;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.client.StatClient;
import ru.practicum.explorewithme.dto.EndpointHit;
import ru.practicum.explorewithme.dto.ViewStats;
import ru.practicum.explorewithme.dto.category.CategoryDto;
import ru.practicum.explorewithme.dto.event.*;
import ru.practicum.explorewithme.exception.BadRequestException;
import ru.practicum.explorewithme.exception.ConflictDataException;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.mapper.EventMapper;
import ru.practicum.explorewithme.mapper.LocationMapper;
import ru.practicum.explorewithme.model.category.Category;
import ru.practicum.explorewithme.model.event.Event;
import ru.practicum.explorewithme.model.event.State;
import ru.practicum.explorewithme.model.event.StateAction;
import ru.practicum.explorewithme.model.location.Location;
import ru.practicum.explorewithme.model.request.Request;
import ru.practicum.explorewithme.model.request.Status;
import ru.practicum.explorewithme.model.user.User;
import ru.practicum.explorewithme.repository.EventRepository;
import ru.practicum.explorewithme.repository.UserRepository;
import ru.practicum.explorewithme.service.category.CategoryService;
import ru.practicum.explorewithme.service.request.RequestService;

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
    private final UserRepository userRepository;
    private final RequestService requestService;
    private final CategoryService categoryService;
    private final StatClient statClient;
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
                predicates.add(root.get("initiator").get("id").in(eventAdminRequest.users()));
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
        Map<Long, Long> amountRequestsByEventIds = requestService.countConfirmedRequestsByEventIds(eventIds);
        Map<Long, Long> viewByEventIds = getNotUniqueStatsByEventIds(eventIds, earliestDate);


        Page<EventFullDto> returnedPage = page.map(event -> {
            Long amountRequest = amountRequestsByEventIds.get(event.getId());
            Long amountRequestResult = amountRequest == null ? 0 : amountRequest;

            Long stat = viewByEventIds.get(event.getId());
            Long resultStat = stat == null ? 0 : stat;

            return EventMapper.toEventFullDto(event, amountRequestResult, resultStat);
        });

        log.info("Return event by param={}", returnedPage);
        return returnedPage;
    }

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        log.info("Try to create event by userId={}", userId);
        User initiator = getUserById(userId);
        CategoryDto categoryDto = categoryService.getCateGoryById(newEventDto.getCategory());
        checkParticipantLimit(newEventDto.getParticipantLimit());

        Event event = Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(new Category(categoryDto.id(), categoryDto.name()))
                .createdOn(LocalDateTime.now())
                .description(newEventDto.getDescription())
                .eventDate(parseDate(newEventDto.getEventDate()))
                .initiator(initiator)
                .location(LocationMapper.mapToLocation(newEventDto.getLocation()))
                .paid(newEventDto.getPaid())
                .participantLimit(newEventDto.getParticipantLimit())
                .publishedOn(null)
                .requestModeration(newEventDto.getRequestModeration())
                .state(State.PENDING)
                .title(newEventDto.getTitle())
                .build();

        Event savedEvent = eventRepository.save(event);
        return EventMapper.toEventFullDto(savedEvent, 0L, 0L);
    }

    @Override
    public EventFullDto getByUserIdAndId(Long userId, Long eventId) {
        Event event = getEventByUserIdAndIdOrThrow(userId, eventId);
        Long confirmedRequests = requestService.countConfirmedRequestsByEventId(eventId);
        Long views = getNotUniqueStatsByEventId(eventId, event.getCreatedOn());
        return EventMapper.toEventFullDto(event, confirmedRequests, views);
    }

    @Override
    public List<EventShortDto> getByUserId(Long userId, int from, int size) {
        log.info("Try to ger List<EventShortDto> by userId={}, from={}, size={}", userId, from, size);
        getUserById(userId);
        PageRequest pageRequest = PageRequest.of(from > 0 ? from / size : 0, size);
        Page<Event> page = eventRepository.findAllByInitiatorId(userId, pageRequest);
        if (page.isEmpty()) {
            return List.of();
        }

        LocalDateTime earliestDate = getEarliestDateInPage(page);
        Set<Long> eventIds = getEventId(page);
        Map<Long, Long> amountRequestsByEventIds = requestService.countConfirmedRequestsByEventIds(eventIds);
        Map<Long, Long> viewByEventIds = getNotUniqueStatsByEventIds(eventIds, earliestDate);

        return page.getContent().stream()
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        amountRequestsByEventIds.getOrDefault(event.getId(), 0L),
                        viewByEventIds.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    @Override
    public List<EventShortDto> searchPublicEvents(String text, List<Long> categories, Boolean paid,
                                                  String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                                  String sort, int from, int size, String requestUri, String ip) {
        log.info("Try to searchPublicEvents by param text={}, categoties={}, paid={}", text, categories, paid);
        LocalDateTime start = parsePublicRangeStart(rangeStart, rangeEnd);
        LocalDateTime end = parseNullableDate(rangeEnd);

        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("Range start must be before range end");
        }

        log.info("Try to find public event in repository");

        Specification<Event> specification = createSpecificationByParam(text, categories,
                paid, start, end, onlyAvailable);

        List<Event> events = eventRepository.findAll(specification);

        log.info("Found event in repository.");

        if (events.isEmpty()) {
            saveHit(requestUri, ip);
            return List.of();
        }

        Set<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toSet());
        Map<Long, Long> confirmedRequestsByEventIds = requestService.countConfirmedRequestsByEventIds(eventIds);
        Map<Long, Long> viewsByEventIds = getNotUniqueStatsByEventIds(eventIds, getEarliestDate(events));
        Comparator<Event> comparator = getPublicSortComparator(sort, viewsByEventIds);

        List<EventShortDto> result = events.stream()
                .sorted(comparator)
                .skip(from)
                .limit(size)
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        confirmedRequestsByEventIds.getOrDefault(event.getId(), 0L),
                        viewsByEventIds.getOrDefault(event.getId(), 0L)))
                .toList();
        log.info("Return List<EventShortDto>.");
        saveHit(requestUri, ip);
        log.info("Saved hit.");
        return result;
    }

    @Override
    public EventFullDto getPublishedEventById(Long eventId, String requestUri, String ip) {
        Event event = eventRepository.findByIdAndState(eventId, State.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        Long confirmedRequests = requestService.countConfirmedRequestsByEventId(eventId);
        Long views = getNotUniqueStatsByEventId(eventId, event.getCreatedOn());
        saveHit(requestUri, ip);
        return EventMapper.toEventFullDto(event, confirmedRequests, views);
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

        Event savedEvent = eventRepository.save(event);
        Long confirmedRequests = requestService.countConfirmedRequestsByEventId(savedEvent.getId());
        Long views = getNotUniqueStatsByEventId(savedEvent.getId(), savedEvent.getCreatedOn());
        return EventMapper.toEventFullDto(savedEvent, confirmedRequests, views);
    }

    private void checkParticipantLimit(Integer participantLimit) {
        if (participantLimit != null && participantLimit < 0) {
            log.error("ParticipantLimit can't be negative ={}", participantLimit);
            throw new BadRequestException("ParticipantLimit can't be negative");
        }
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

        Long amountRequestsByEventId = requestService.countConfirmedRequestsByEventId(savedEvent.getId());
        Long viewByEventId = getNotUniqueStatsByEventId(savedEvent.getId(), savedEvent.getCreatedOn());

        return EventMapper.toEventFullDto(savedEvent, amountRequestsByEventId, viewByEventId);
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

    private Map<Long, Long> getNotUniqueStatsByEventIds(Set<Long> eventIds, LocalDateTime from) {
        log.info("Try to get getNotUniqueStatsByEventIds eventIds={}, from={}", eventIds, from);
        if (eventIds.isEmpty() || from == null) {
            return Map.of();
        }
        List<String> uris = new ArrayList<>();
        eventIds.forEach(eventId -> uris.add(uri + eventId));

        List<ViewStats> viewStats = statClient.getStat(from, LocalDateTime.now(), uris, true);
        if (viewStats.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, Long> map = viewStats.stream()
                .collect(Collectors.toMap(
                        viewStat -> {
                            String[] parts = viewStat.uri().split("/");
                            String numberStr = parts[parts.length - 1];
                            return Long.parseLong(numberStr);
                        },
                        ViewStats::hits,
                        (existing, replacement) -> existing));
        log.info("Return NotUniqueStatsByEventIds eventIds={}, from={}", eventIds, from);
        return map;
    }

    private Long getNotUniqueStatsByEventId(Long eventId, LocalDateTime from) {
        if (from == null) {
            return 0L;
        }
        List<String> uris = List.of(uri + eventId);

        List<ViewStats> viewStats = statClient.getStat(from, LocalDateTime.now(), uris, true);
        return viewStats.stream()
                .map(ViewStats::hits)
                .mapToLong(Long::longValue)
                .sum();

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

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found by ID=" + userId));
    }

    private Event getEventByUserIdAndIdOrThrow(Long userId, Long eventId) {
        getUserById(userId);
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
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

    private Comparator<Event> getPublicSortComparator(String sort, Map<Long, Long> viewsByEventIds) {
        if (sort == null || sort.isBlank() || sort.equals("EVENT_DATE")) {
            return Comparator.comparing(Event::getEventDate);
        }
        if (sort.equals("VIEWS")) {
            return Comparator.comparingLong((Event event) -> viewsByEventIds.getOrDefault(event.getId(), 0L))
                    .reversed();
        }
        throw new BadRequestException("Unknown sort option=" + sort);
    }

    private void saveHit(String requestUri, String ip) {
        statClient.saveHit(new EndpointHit(null, appName, requestUri, ip, LocalDateTime.now()));
    }

    private Specification<Event> createSpecificationByParam(String text, List<Long> categories,
                                                            Boolean paid, LocalDateTime start,
                                                            LocalDateTime end, Boolean onlyAvailable) {

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

            if (Boolean.TRUE.equals(onlyAvailable)) {
                Subquery<Long> requestCount = query.subquery(Long.class);
                Root<Request> requestRoot = requestCount.from(Request.class);
                requestCount.select(criteriaBuilder.count(requestRoot))
                        .where(
                                criteriaBuilder.equal(requestRoot.get("event"), root),
                                criteriaBuilder.equal(requestRoot.get("status"), Status.CONFIRMED)
                        );

                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.or(
                                criteriaBuilder.isNull(root.get("participantLimit")),
                                criteriaBuilder.equal(root.get("participantLimit"), 0),
                                criteriaBuilder.greaterThan(root.get("participantLimit"), requestCount)
                        )
                );
            }

            return predicate;
        };
    }
}
