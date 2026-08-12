package ru.practicum.explorewithme.service.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.client.StatClient;
import ru.practicum.explorewithme.dto.ViewStats;
import ru.practicum.explorewithme.dto.event.EventAdminRequest;
import ru.practicum.explorewithme.dto.event.EventFullDto;
import ru.practicum.explorewithme.dto.event.UpdateEventAdminRequest;
import ru.practicum.explorewithme.exception.ConflictDataException;
import ru.practicum.explorewithme.model.category.Category;
import ru.practicum.explorewithme.model.event.Event;
import ru.practicum.explorewithme.model.event.State;
import ru.practicum.explorewithme.model.location.Location;
import ru.practicum.explorewithme.model.user.User;
import ru.practicum.explorewithme.repository.CategoryRepository;
import ru.practicum.explorewithme.repository.EventRepository;
import ru.practicum.explorewithme.repository.LocationRepository;
import ru.practicum.explorewithme.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventServiceImplTest {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationRepository locationRepository;

    @MockBean
    private StatClient statClient;

    private Event firstEvent;
    private Event secondEvent;
    private Category savedCategory2;

    @BeforeEach
    void createDataInDB() {
        Category category = Category.builder()
                .id(1L)
                .name("category")
                .build();
        Category savedCategory = categoryRepository.save(category);

        Category category2 = Category.builder()
                .id(2L)
                .name("second_category")
                .build();
        savedCategory2 = categoryRepository.save(category2);

        User user = User.builder()
                .id(1L)
                .name("user")
                .email("user@test.ru")
                .build();
        User savedUser = userRepository.save(user);

        Location location = Location.builder()
                .id(1L)
                .lat(1.1f)
                .lon(2.2f)
                .build();
        Location savedLocation = locationRepository.save(location);

        firstEvent = Event.builder()
                .id(1L)
                .annotation("first_annotation")
                .category(savedCategory)
                .createdOn(LocalDateTime.now().minusHours(1))
                .description("first_description")
                .eventDate(LocalDateTime.now().plusHours(1))
                .initiator(savedUser)
                .location(savedLocation)
                .paid(true)
                .participantLimit(10)
                .publishedOn(LocalDateTime.now())
                .requestModeration(true)
                .state(State.PENDING)
                .title("test_title")
                .build();

        secondEvent = Event.builder()
                .id(2L)
                .annotation("second_annotation")
                .category(savedCategory2)
                .createdOn(LocalDateTime.now().minusDays(1).minusHours(1))
                .description("first_description")
                .eventDate(LocalDateTime.now().minusDays(1).plusHours(1))
                .initiator(savedUser)
                .location(savedLocation)
                .paid(true)
                .participantLimit(10)
                .publishedOn(LocalDateTime.now().minusDays(1))
                .requestModeration(true)
                .state(State.PUBLISHED)
                .title("second_event_test_title")
                .build();
    }

    @Test
    void getEventByParamShouldReturnEventsCorrectly() {
        Event savedEvent = eventRepository.save(firstEvent);

        List<Long> users = List.of(1L, 2L, 3L);
        List<String> states = List.of("PENDING");
        List<Long> categories = List.of(1L, 2L, 3L);
        String rangeStart = LocalDateTime.now().minusDays(1).format(formatter);
        String rangeEnd = LocalDateTime.now().plusDays(1).format(formatter);
        EventAdminRequest request = new EventAdminRequest(
                users, states, categories, rangeStart, rangeEnd
        );

        Pageable pageable = PageRequest.of(0 / 10, 10);

        List<String> uris = List.of("/events/" + savedEvent.getId());
        ViewStats viewStats = new ViewStats("", uris.getFirst(), 100L);

        when(statClient.getStat(Mockito.any(LocalDateTime.class), Mockito.any(LocalDateTime.class),
                Mockito.anyList(), Mockito.eq(true)))
                .thenReturn(List.of(viewStats));

        Page<EventFullDto> receivedPage = eventService.getEventByParam(request, pageable);
        EventFullDto receivedEvent = receivedPage.getContent().getFirst();

        assertNotNull(receivedPage);
        assertEquals(1, receivedPage.getTotalPages());
        assertEquals(1, receivedPage.getTotalElements());
        assertEquals(0, receivedPage.getNumber()); // текущая страница (0-indexed)
        assertEquals(10, receivedPage.getSize()); // размер страницы
        assertEquals(1, receivedPage.getNumberOfElements());
        assertEquals(savedEvent.getId(), receivedEvent.getId());
        assertEquals(savedEvent.getId(), receivedEvent.getId());
        assertEquals(savedEvent.getTitle(), receivedEvent.getTitle());
        assertEquals(savedEvent.getAnnotation(), receivedEvent.getAnnotation());
        assertEquals(savedEvent.getDescription(), receivedEvent.getDescription());
        assertEquals(100L, receivedEvent.getViews());
    }

    @Test
    void getEventByParamShouldReturnEmptyPageWithAnotherPage() {
        Event savedEvent = eventRepository.save(firstEvent);

        List<Long> users = List.of(1L, 2L, 3L);
        List<String> states = List.of("PENDING");
        List<Long> categories = List.of(1L, 2L, 3L);
        String rangeStart = LocalDateTime.now().minusDays(1).format(formatter);
        String rangeEnd = LocalDateTime.now().plusDays(1).format(formatter);
        EventAdminRequest request = new EventAdminRequest(
                users, states, categories, rangeStart, rangeEnd
        );

        Pageable pageable = PageRequest.of(1, 10);

        List<String> uris = List.of("/events/" + savedEvent.getId());
        ViewStats viewStats = new ViewStats("", uris.getFirst(), 100L);

        when(statClient.getStat(Mockito.any(LocalDateTime.class), Mockito.any(LocalDateTime.class),
                Mockito.anyList(), Mockito.eq(true)))
                .thenReturn(List.of(viewStats));

        Page<EventFullDto> receivedPage = eventService.getEventByParam(request, pageable);


        assertNotNull(receivedPage);
        assertTrue(receivedPage.getContent().isEmpty());
        assertEquals(0, receivedPage.getTotalPages());
        assertEquals(0, receivedPage.getTotalElements());
        assertEquals(1, receivedPage.getNumber());
        assertEquals(10, receivedPage.getSize());
    }

    @Test
    void getEventByParamShouldReturnEmptyPageWithAnotherRangeStart() {
        Event savedEvent = eventRepository.save(firstEvent);

        List<Long> users = List.of(1L, 2L, 3L);
        List<String> states = List.of("PENDING");
        List<Long> categories = List.of(1L, 2L, 3L);
        String rangeStart = LocalDateTime.now().plusDays(1).format(formatter);
        String rangeEnd = LocalDateTime.now().plusDays(2).format(formatter);
        EventAdminRequest request = new EventAdminRequest(
                users, states, categories, rangeStart, rangeEnd
        );

        Pageable pageable = PageRequest.of(0 / 10, 10);

        List<String> uris = List.of("/events/" + savedEvent.getId());
        ViewStats viewStats = new ViewStats("", uris.getFirst(), 100L);

        when(statClient.getStat(Mockito.any(LocalDateTime.class), Mockito.any(LocalDateTime.class),
                Mockito.anyList(), Mockito.eq(true)))
                .thenReturn(List.of(viewStats));

        Page<EventFullDto> receivedPage = eventService.getEventByParam(request, pageable);

        assertNotNull(receivedPage);
        assertTrue(receivedPage.getContent().isEmpty());
        assertEquals(0, receivedPage.getTotalPages());
        assertEquals(0, receivedPage.getTotalElements());
        assertEquals(0, receivedPage.getNumber());
        assertEquals(10, receivedPage.getSize());
    }

    @Test
    void getEventByParamShouldReturnEmptyPageWithAnotherStates() {
        Event savedEvent = eventRepository.save(firstEvent);

        List<Long> users = List.of(1L, 2L, 3L);
        List<String> states = List.of("CANCELED");
        List<Long> categories = List.of(1L, 2L, 3L);
        String rangeStart = LocalDateTime.now().minusDays(1).format(formatter);
        String rangeEnd = LocalDateTime.now().plusDays(2).format(formatter);
        EventAdminRequest request = new EventAdminRequest(
                users, states, categories, rangeStart, rangeEnd
        );

        Pageable pageable = PageRequest.of(0 / 10, 10);

        List<String> uris = List.of("/events/" + savedEvent.getId());
        ViewStats viewStats = new ViewStats("", uris.getFirst(), 100L);

        when(statClient.getStat(Mockito.any(LocalDateTime.class), Mockito.any(LocalDateTime.class),
                Mockito.anyList(), Mockito.eq(true)))
                .thenReturn(List.of(viewStats));

        Page<EventFullDto> receivedPage = eventService.getEventByParam(request, pageable);

        assertNotNull(receivedPage);
        assertTrue(receivedPage.getContent().isEmpty());
        assertEquals(0, receivedPage.getTotalPages());
        assertEquals(0, receivedPage.getTotalElements());
        assertEquals(0, receivedPage.getNumber());
        assertEquals(10, receivedPage.getSize());
    }

    @Test
    void updateEventAdminShouldUpdateFullEventCorrectly() {
        Event savedEvent = eventRepository.save(firstEvent);
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .annotation("test_annotation".repeat(5))
                .category(savedCategory2.getId())
                .description("test_description".repeat(5))
                .eventDate(LocalDateTime.now().plusHours(3).format(formatter))
                .location(new Location(30L, 1.1f, 2.2f))
                .paid(true)
                .participantLimit(40)
                .requestModeration(true)
                .stateAction("PUBLISH_EVENT")
                .title("test_title")
                .build();

        EventFullDto receivedEvent = eventService.updateEventAdmin(savedEvent.getId(), request);

        assertEquals("test_annotation".repeat(5), receivedEvent.getAnnotation());
        assertEquals(savedCategory2.getId(), receivedEvent.getCategory().id());
        assertEquals("test_description".repeat(5), receivedEvent.getDescription());
        assertEquals(request.getEventDate(), receivedEvent.getEventDate());
    }

    @Test
    void updateEventAdminShouldUpdatePartlyEventCorrectly() {
        Event savedEvent = eventRepository.save(firstEvent);
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .annotation("test_annotation".repeat(5))
                .category(savedCategory2.getId())
                .description("test_description".repeat(5))
                .eventDate(LocalDateTime.now().plusHours(3).format(formatter))
                .build();

        EventFullDto receivedEvent = eventService.updateEventAdmin(savedEvent.getId(), request);

        assertEquals("test_annotation".repeat(5), receivedEvent.getAnnotation());
        assertEquals(savedCategory2.getId(), receivedEvent.getCategory().id());
        assertEquals("test_description".repeat(5), receivedEvent.getDescription());
        assertEquals(request.getEventDate(), receivedEvent.getEventDate());
        assertEquals(savedEvent.getTitle(), receivedEvent.getTitle());
        assertEquals(savedEvent.getInitiator().getName(), receivedEvent.getInitiator().getName());
    }

    @Test
    void updateEventAdminShouldThrowConflictExceptionWhenUpdatePublishState() {
        Event savedEvent = eventRepository.save(secondEvent);
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .annotation("test_annotation".repeat(15))
                .category(savedCategory2.getId())
                .description("test_description".repeat(20))
                .eventDate(LocalDateTime.now().plusHours(3).format(formatter))
                .location(new Location(30L, 1.1f, 2.2f))
                .paid(true)
                .participantLimit(40)
                .requestModeration(true)
                .stateAction("PUBLISH_EVENT")
                .title("test_title")
                .build();

        ConflictDataException exception = assertThrows(ConflictDataException.class,
                () -> eventService.updateEventAdmin(savedEvent.getId(), request));
        assertEquals("Cannot publish the event because it's not in the right state", exception.getMessage());
    }

    @Test
    void updateEventAdminShouldThrowConflictExceptionWhenUpdateIncorrectEventDate() {
        Event savedEvent = eventRepository.save(firstEvent);
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .annotation("test_annotation".repeat(15))
                .category(savedCategory2.getId())
                .description("test_description".repeat(20))
                .eventDate(LocalDateTime.now().plusMinutes(3).format(formatter))
                .location(new Location(30L, 1.1f, 2.2f))
                .paid(true)
                .participantLimit(40)
                .requestModeration(true)
                .stateAction("PUBLISH_EVENT")
                .title("test_title")
                .build();

        ConflictDataException exception = assertThrows(ConflictDataException.class,
                () -> eventService.updateEventAdmin(savedEvent.getId(), request));
        assertTrue(exception.getMessage().contains("event date can't be earlier than="));
    }
}
