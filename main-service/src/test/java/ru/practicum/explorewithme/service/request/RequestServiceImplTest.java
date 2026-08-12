package ru.practicum.explorewithme.service.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.dto.request.ParticipationRequestDto;
import ru.practicum.explorewithme.exception.ConflictDataException;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.model.category.Category;
import ru.practicum.explorewithme.model.event.Event;
import ru.practicum.explorewithme.model.event.State;
import ru.practicum.explorewithme.model.location.Location;
import ru.practicum.explorewithme.model.request.Request;
import ru.practicum.explorewithme.model.request.Status;
import ru.practicum.explorewithme.model.user.User;
import ru.practicum.explorewithme.repository.*;
import ru.practicum.explorewithme.repository.request.RequestRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RequestServiceImplTest {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RequestService requestService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Event savedEvent;
    private Event savedEvent2;
    private Event savedEvent3;
    private Event savedEvent4;
    private User savedRequester;
    private User eventOwner;
    private Request savedRequest;

    @BeforeEach
    void setUp() {
        User user = new User(1L, "user", "email");
        eventOwner = userRepository.save(user);

        Category category = new Category(1L, "category");
        Category savedCategory = categoryRepository.save(category);

        Location location = new Location(1L, 123f, 123f);
        Location savedLocation = locationRepository.save(location);

        Event event = new Event(1L, "annotation", savedCategory, LocalDateTime.now().minusHours(2),
                "description", LocalDateTime.now(), eventOwner, savedLocation, true, 10,
                LocalDateTime.now().minusHours(1), true, State.PUBLISHED, "title");
        savedEvent = eventRepository.save(event);

        Event event2 = new Event(2L, "annotation", savedCategory, LocalDateTime.now().minusHours(2),
                "description", LocalDateTime.now(), eventOwner, savedLocation, true, 10,
                LocalDateTime.now().minusHours(1), true, State.PENDING, "title");
        savedEvent2 = eventRepository.save(event2);

        Event event3 = new Event(3L, "annotation", savedCategory, LocalDateTime.now().minusHours(2),
                "description", LocalDateTime.now(), eventOwner, savedLocation, true, 1,
                LocalDateTime.now().minusHours(1), true, State.PUBLISHED, "title");
        savedEvent3 = eventRepository.save(event3);

        Event event4 = new Event(4L, "annotation", savedCategory, LocalDateTime.now().minusHours(2),
                "description", LocalDateTime.now(), eventOwner, savedLocation, true, 10,
                LocalDateTime.now().minusHours(1), false, State.PUBLISHED, "title");
        savedEvent4 = eventRepository.save(event4);

        User requester = new User(2L, "requester", "email");
        savedRequester = userRepository.save(requester);

        Request firstRequest = new Request(1L, LocalDateTime.now(), savedEvent, savedRequester, Status.PENDING);
        savedRequest = requestRepository.save(firstRequest);

        Request secondRequest = new Request(2L, LocalDateTime.now().plusMinutes(1), savedEvent2,
                savedRequester, Status.PENDING);
        requestRepository.save(secondRequest);

        Request thirdRequest = new Request(3L, LocalDateTime.now().plusMinutes(2), savedEvent3,
                savedRequester, Status.CONFIRMED);
        requestRepository.save(thirdRequest);
    }

    @Test
    void countRequestsByEventIdsShouldContCorrectly() {
        Map<Long, Long> mapEventIdAmountRequest = requestService.countRequestsByEventIds(Set.of(savedEvent.getId()));

        assertEquals(1L, mapEventIdAmountRequest.get(savedEvent.getId()));
    }

    @Test
    void countRequestsByEventIdsShouldReturnEmptyMapWhenNoRequestByEventIds() {
        Map<Long, Long> mapEventIdAmountRequest = requestService.countRequestsByEventIds(Set.of(1000L));

        assertTrue(mapEventIdAmountRequest.isEmpty());
    }

    @Test
    void getUserRequestsShouldReturnListCorrectly() {
        List<ParticipationRequestDto> expectedList = requestService.getUserRequests(savedRequester.getId());

        assertEquals(3, expectedList.size());
        assertEquals(savedEvent.getId(), expectedList.getFirst().event());
        assertEquals(savedEvent2.getId(), expectedList.get(1).event());
        assertEquals(savedEvent3.getId(), expectedList.getLast().event());
    }

    @Test
    void getUserRequestsShouldReturnEmptyListWhenNoRequestFromRequestor() {
        User user = new User(100L, "test_user", "test@email.user");
        User savedUser = userRepository.save(user);
        List<ParticipationRequestDto> expectedList = requestService.getUserRequests(savedUser.getId());

        assertTrue(expectedList.isEmpty());
    }

    @Test
    void countRequestsByEventIdShouldContCorrectly() {
        Long amountRequests = requestService.countRequestsByEventId(savedEvent.getId());

        assertEquals(1L, amountRequests);
    }

    @Test
    void countRequestsByEventIdShouldReturnZeroWhenNotRequestNyEventId() {
        Long amountRequests = requestService.countRequestsByEventId(1000L);

        assertEquals(0L, amountRequests);
    }

    @Test
    void addUserRequestShouldCreateRequestCorrectly() {
        User newUser = new User(null, "user_for_this_test", "email@for_this_user.ru");
        User savedUser = userRepository.save(newUser);
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(1);
        ParticipationRequestDto result = requestService.addUserRequest(savedUser.getId(), savedEvent.getId());
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(1);

        LocalDateTime resultDateTime = LocalDateTime.parse(result.created(), formatter);

        assertNotNull(result.id());
        assertTrue(resultDateTime.isAfter(startTime) && resultDateTime.isBefore(endTime));
        assertEquals(savedUser.getId(), result.requester());
        assertEquals(savedEvent.getId(), result.event());
        assertEquals(Status.PENDING.name(), result.status());
    }

    @Test
    void addUserRequestShouldThrowNotFoundExceptionWhenIncorrectUserId() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> requestService.addUserRequest(999L, savedEvent.getId()));

        assertEquals("User was not found with id=" + 999L, exception.getMessage());

    }

    @Test
    void addUserRequestShouldThrowNotFoundExceptionWhenIncorrectEventId() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> requestService.addUserRequest(savedRequester.getId(), 999L));

        assertEquals("Event was not found with id=" + 999L, exception.getMessage());
    }

    @Test
    void addUserRequestShouldThrowConflictExceptionWhenUserIsEventOwner() {
        ConflictDataException exception = assertThrows(ConflictDataException.class,
                () -> requestService.addUserRequest(eventOwner.getId(), savedEvent.getId()));

        assertEquals("Requester cant make request for its event", exception.getMessage());
    }

    @Test
    void addUserRequestShouldThrowConflictExceptionWhenUserMadeMoreThanOneRequest() {
        User newUser = new User(null, "user_for_this_test", "email@for_this_user.ru");
        User savedUser = userRepository.save(newUser);
        requestService.addUserRequest(savedUser.getId(), savedEvent.getId());

        ConflictDataException exception = assertThrows(ConflictDataException.class,
                () -> requestService.addUserRequest(savedUser.getId(), savedEvent.getId()));

        assertEquals("Request with requesterId and eventId is already in DB", exception.getMessage());
    }

    @Test
    void addUserRequestShouldThrowConflictExceptionWhenUserMadeRequestForNotPublishedEvent() {
        User newUser = new User(null, "user_for_this_test", "email@for_this_user.ru");
        User savedUser = userRepository.save(newUser);

        ConflictDataException exception = assertThrows(ConflictDataException.class,
                () -> requestService.addUserRequest(savedUser.getId(), savedEvent2.getId()));

        assertEquals("Event=" + savedEvent2.getId() + " is not published", exception.getMessage());
    }

    @Test
    void addUserRequestShouldThrowConflictExceptionWhenEventFullParticipation() {
        User newUser = new User(null, "user_for_this_test", "email@for_this_user.ru");
        User savedUser = userRepository.save(newUser);

        ConflictDataException exception = assertThrows(ConflictDataException.class,
                () -> requestService.addUserRequest(savedUser.getId(), savedEvent3.getId()));

        assertEquals("Event=" + savedEvent3.getId() + " has no available spots for participation",
                exception.getMessage());
    }

    @Test
    void addUserRequestShouldCreateRequestCorrectlyWithStatusConfirmed() {
        User newUser = new User(null, "user_for_this_test", "email@for_this_user.ru");
        User savedUser = userRepository.save(newUser);
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(1);
        ParticipationRequestDto result = requestService.addUserRequest(savedUser.getId(), savedEvent4.getId());
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(1);

        LocalDateTime resultDateTime = LocalDateTime.parse(result.created(), formatter);

        assertNotNull(result.id());
        assertTrue(resultDateTime.isAfter(startTime) && resultDateTime.isBefore(endTime));
        assertEquals(savedUser.getId(), result.requester());
        assertEquals(savedEvent4.getId(), result.event());
        assertEquals(Status.CONFIRMED.name(), result.status());
    }

    @Test
    void rejectUserRequestShouldUpdateRequestCorrectly() {
        ParticipationRequestDto result = requestService.rejectUserRequest(savedRequester.getId(), savedRequest.getId());

        assertEquals(savedRequest.getId(), result.id());
        assertEquals(savedRequest.getCreated().format(formatter), result.created());
        assertEquals(savedRequest.getEvent().getId(), result.event());
        assertEquals(savedRequest.getRequester().getId(), result.requester());
        assertEquals(savedRequest.getRequester().getId(), result.requester());
        assertEquals(Status.CANCELED.name(), result.status());
    }

    @Test
    void rejectUserRequestShouldThrowNotFoundExceptionWhenUserIdNoInDB() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> requestService.rejectUserRequest(1000L, savedRequest.getId()));

        assertEquals("User was not found with id=" + 1000L,
                exception.getMessage());
    }

    @Test
    void rejectUserRequestShouldThrowNotFoundExceptionWhenRequestIdNoInDB() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> requestService.rejectUserRequest(savedRequester.getId(), 1000L));

        assertEquals("Request was not found with id=" + 1000L,
                exception.getMessage());
    }

    @Test
    void rejectUserRequestShouldThrowConflictDataExceptionWhenRequestRejectNotRequestor() {
        User newUser = new User(null, "user_for_this_test", "email@for_this_user.ru");
        User savedUser = userRepository.save(newUser);

        ConflictDataException exception = assertThrows(ConflictDataException.class,
                () -> requestService.rejectUserRequest(savedUser.getId(), savedRequest.getId()));

        assertEquals("Canceled request can only requestor",
                exception.getMessage());
    }
}
