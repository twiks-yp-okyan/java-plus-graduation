package ru.practicum.explorewithme.service.compilation;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.client.AnalyzerClient;
import ru.practicum.explorewithme.dto.compilation.CompilationDto;
import ru.practicum.explorewithme.dto.compilation.NewCompilationDto;
import ru.practicum.explorewithme.dto.compilation.UpdateCompilationRequest;
import ru.practicum.explorewithme.dto.event.EventShortDto;
import ru.practicum.explorewithme.dto.user.UserDto;
import ru.practicum.explorewithme.exception.ConflictDataException;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.feign.RequestClient;
import ru.practicum.explorewithme.feign.UserClient;
import ru.practicum.explorewithme.mapper.CompilationMapper;
import ru.practicum.explorewithme.mapper.EventMapper;
import ru.practicum.explorewithme.model.compilation.Compilation;
import ru.practicum.explorewithme.model.event.Event;
import ru.practicum.explorewithme.repository.CompilationRepository;
import ru.practicum.explorewithme.repository.EventRepository;
import ru.yandex.practicum.grpc.message.RecommendedEventProto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final RequestClient requestClient;
    private final UserClient userClient;
    private final AnalyzerClient analyzerClient;

    private static final String EVENT_URI_PREFIX = "/events/";

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        log.info("Try to create compilation with title={}", newCompilationDto.getTitle());
        Compilation compilation = Compilation.builder()
                .events(getEventsByIds(newCompilationDto.getEvents()))
                .pinned(Boolean.TRUE.equals(newCompilationDto.getPinned()))
                .title(newCompilationDto.getTitle())
                .build();
        try {
            Compilation savedCompilation = compilationRepository.save(compilation);
            return toCompilationDto(savedCompilation);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictDataException("Compilation already exists");
        }
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        log.info("Try to delete compilation by id={}", compId);
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }
        compilationRepository.deleteById(compId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        log.info("Try to update compilation by id={}", compId);
        Compilation compilation = getCompilationOrThrow(compId);

        if (updateCompilationRequest.getEvents() != null) {
            compilation.setEvents(getEventsByIds(updateCompilationRequest.getEvents()));
        }
        if (updateCompilationRequest.getPinned() != null) {
            compilation.setPinned(updateCompilationRequest.getPinned());
        }
        if (updateCompilationRequest.getTitle() != null) {
            compilation.setTitle(updateCompilationRequest.getTitle());
        }

        try {
            Compilation savedCompilation = compilationRepository.save(compilation);
            return toCompilationDto(savedCompilation);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictDataException("Compilation already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {

        int page = from > 0 ? from / size : 0;
        Pageable pageable = PageRequest.of(page, size);

        Page<Compilation> compilations;

        if (pinned != null) {
            compilations = compilationRepository.findAllByPinned(pinned, pageable);
        } else {
            compilations = compilationRepository.findAll(pageable);
        }

        return compilations.stream()
                .map(this::toCompilationDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilation(Long compId) {
        Compilation compilation = getCompilationOrThrow(compId);
        return toCompilationDto(compilation);
    }

    private Compilation getCompilationOrThrow(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
    }

    private LinkedHashSet<Event> getEventsByIds(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new LinkedHashSet<>();
        }

        List<Event> events = eventRepository.findAllById(eventIds);
        Map<Long, Event> eventsById = new LinkedHashMap<>();
        for (Event event : events) {
            eventsById.put(event.getId(), event);
        }

        LinkedHashSet<Event> orderedEvents = new LinkedHashSet<>();
        for (Long eventId : eventIds) {
            Event event = eventsById.get(eventId);
            if (event == null) {
                throw new NotFoundException("Event with id=" + eventId + " was not found");
            }
            orderedEvents.add(event);
        }
        return orderedEvents;
    }

    private CompilationDto toCompilationDto(Compilation compilation) {
        Set<Long> eventIds = compilation.getEvents().stream()
                .map(Event::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Map<Long, Integer> requestsByEventIds = requestClient.getRequestsCountByEventIds(eventIds);
        Map<Long, Double> eventsRating = getEventsRating(compilation.getEvents());

        LinkedHashSet<EventShortDto> events = compilation.getEvents().stream()
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        getUserById(event.getInitiatorId()),
                        requestsByEventIds.getOrDefault(event.getId(), 0),
                        eventsRating.getOrDefault(event.getId(), 0.0)))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return CompilationMapper.toCompilationDto(compilation, events);
    }

    private Map<Long, Double> getEventsRating(Set<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        return analyzerClient.getInteractionsCount(eventIds)
                .collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore));
    }

    private UserDto getUserById(Long userId) {
        try {
            log.debug("Попытка получить пользователя из user-service по id = {}", userId);
            return userClient.getById(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("User not found by ID=" + userId);
        }
    }
}
