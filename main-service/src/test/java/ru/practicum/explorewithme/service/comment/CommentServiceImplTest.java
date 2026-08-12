package ru.practicum.explorewithme.service.comment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.dto.comment.CommentDto;
import ru.practicum.explorewithme.dto.comment.NewComment;
import ru.practicum.explorewithme.exception.ConflictDataException;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.model.category.Category;
import ru.practicum.explorewithme.model.comment.Comment;
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
import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommentServiceImplTest {
    private final DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private CommentService commentService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private CommentRepository commentRepository;

    private User eventVisitor;

    private User eventInitiator;

    private Event savedEvent;

    private Request savedRequest;

    private String commentText;

    private NewComment newComment;

    @BeforeEach
    void createDataInDB() {
        Category category = Category.builder()
                .name("category")
                .build();
        Category savedCategory = categoryRepository.save(category);

        User user1 = User.builder()
                .name("initiator_user")
                .email("initiator_user@test.ru")
                .build();
        eventInitiator = userRepository.save(user1);

        User user2 = User.builder()
                .name("requestor_user")
                .email("requestor_user@test.ru")
                .build();
        eventVisitor = userRepository.save(user2);

        Location location = Location.builder()
                .lat(1.1f)
                .lon(2.2f)
                .build();
        Location savedLocation = locationRepository.save(location);

        Event event1 = Event.builder()
                .annotation("first_annotation")
                .category(savedCategory)
                .createdOn(LocalDateTime.now().minusHours(5))
                .description("first_description")
                .eventDate(LocalDateTime.now().minusHours(3))
                .initiator(eventInitiator)
                .location(savedLocation)
                .paid(true)
                .participantLimit(10)
                .publishedOn(LocalDateTime.now().minusHours(4))
                .requestModeration(true)
                .state(State.PENDING)
                .title("test_title")
                .build();
        savedEvent = eventRepository.save(event1);

        Request request = Request.builder()
                .created(savedEvent.getPublishedOn().plusMinutes(5))
                .event(savedEvent)
                .requester(eventVisitor)
                .status(Status.CONFIRMED)
                .build();
        savedRequest = requestRepository.save(request);

        commentText = "test comment".repeat(20);
        newComment = new NewComment(commentText);
    }

    @Test
    void createShouldCreateCommentCorrectly() throws Exception {
        CommentDto resultComment = commentService.create(eventVisitor.getId(), savedEvent.getId(), newComment);

        Assertions.assertNotNull(resultComment.getId());
        Assertions.assertEquals(commentText, resultComment.getText());
        Assertions.assertEquals(savedEvent.getId(), resultComment.getEvent());
        Assertions.assertEquals(eventVisitor.getId(), resultComment.getUser());
        Assertions.assertTrue(LocalDateTime.parse(resultComment.getCreatedOn(), customFormatter)
                .isAfter(LocalDateTime.now().minusHours(1)));
    }

    @Test
    void createShouldThrowNotFoundWhenNoUserInDB() throws Exception {
        NotFoundException exception = Assertions.assertThrows(NotFoundException.class,
                () -> commentService.create(1000L, savedEvent.getId(), newComment));
        Assertions.assertTrue(exception.getMessage().contains("Couldn't find user by id=1000"));
    }

    @Test
    void createShouldThrowNotFoundWhenNoEventInDB() throws Exception {
        NotFoundException exception = Assertions.assertThrows(NotFoundException.class,
                () -> commentService.create(eventVisitor.getId(), 1000L, newComment));
        Assertions.assertTrue(exception.getMessage().contains("Couldn't find event by id=1000"));
    }

    @Test
    void createShouldThrowConflictWhenEventOwnerCreateComment() throws Exception {
        ConflictDataException exception = Assertions.assertThrows(ConflictDataException.class,
                () -> commentService.create(eventInitiator.getId(), savedEvent.getId(), newComment));
        Assertions.assertTrue(exception.getMessage().contains("User cant create comment for his event"));
    }

    @Test
    void createShouldThrowNotFoundWhenNoRequestFromEventVisitor() throws Exception {
        requestRepository.deleteById(savedRequest.getId());
        NotFoundException exception = Assertions.assertThrows(NotFoundException.class,
                () -> commentService.create(eventVisitor.getId(), savedEvent.getId(), newComment));
        Assertions.assertTrue(exception.getMessage().contains(("Couldn't find request from requestorId="
                + eventVisitor.getId() + " to eventId=" + savedEvent.getId())));
    }

    @Test
    void createShouldThrowConflictWhenRequestNotConfirmed() throws Exception {
        savedRequest.setStatus(Status.REJECTED);
        requestRepository.save(savedRequest);

        ConflictDataException exception = Assertions.assertThrows(ConflictDataException.class,
                () -> commentService.create(eventVisitor.getId(), savedEvent.getId(), newComment));
        Assertions.assertTrue(exception.getMessage().contains(("Create comment can only event's visitor")));
    }

    @Test
    void createShouldThrowConflictWhenCreateOneMoreComment() throws Exception {
        Comment comment = new Comment(100L, commentText, savedEvent, eventVisitor, LocalDateTime.now());
        commentRepository.save(comment);

        ConflictDataException exception = Assertions.assertThrows(ConflictDataException.class,
                () -> commentService.create(eventVisitor.getId(), savedEvent.getId(), newComment));
        Assertions.assertTrue(exception.getMessage().contains(("Comment has already in DB from userId="
                + eventVisitor.getId() + " to eventId=" + savedEvent.getId())));
    }

    @Test
    void updateShouldUpdateCommentCorrectly() throws Exception {
        Long commentId = 100L;

        Comment comment = new Comment(commentId, commentText, savedEvent, eventVisitor, LocalDateTime.now());
        Comment savedComment = commentRepository.save(comment);

        NewComment updateComment = new NewComment("updateInfo".repeat(20));

        CommentDto resultComment = commentService.update(eventVisitor.getId(),
                savedComment.getId(), updateComment);

        Assertions.assertNotNull(resultComment.getId());
        Assertions.assertEquals(updateComment.getText(), resultComment.getText());
        Assertions.assertEquals(savedEvent.getId(), resultComment.getEvent());
        Assertions.assertEquals(eventVisitor.getId(), resultComment.getUser());
        Assertions.assertTrue(LocalDateTime.parse(resultComment.getCreatedOn(), customFormatter)
                .isAfter(LocalDateTime.now().minusHours(1)));
    }

    @Test
    void updateShouldThrowNotFoundWhenIncorrectCommentId() throws Exception {
        Long commentId = 100L;

        NewComment updateComment = new NewComment("updateInfo".repeat(20));

        NotFoundException exception = Assertions.assertThrows(NotFoundException.class,
                () -> commentService.update(eventVisitor.getId(), commentId, updateComment));
        Assertions.assertTrue(exception.getMessage().contains(("Couldn't find comment by id=" + commentId)));
    }

    @Test
    void updateShouldThrowConflictWhenUserIsNotAuthor() throws Exception {
        Long commentId = 100L;

        Comment comment = new Comment(commentId, commentText, savedEvent, eventVisitor, LocalDateTime.now());
        Comment savedComment = commentRepository.save(comment);

        NewComment updateComment = new NewComment("updateInfo".repeat(20));

        ConflictDataException exception = Assertions.assertThrows(ConflictDataException.class,
                () -> commentService.update(1000L, savedComment.getId(), updateComment));
        Assertions.assertTrue(exception.getMessage().contains(("Edit comment can only author")));
    }

    @Test
    void updateShouldThrowConflictWhenCommentNotAvailableToEdit() throws Exception {
        Long commentId = 100L;

        Comment comment = new Comment(commentId, commentText, savedEvent, eventVisitor,
                LocalDateTime.now().minusDays(2));
        Comment savedComment = commentRepository.save(comment);

        NewComment updateComment = new NewComment("updateInfo".repeat(20));

        ConflictDataException exception = Assertions.assertThrows(ConflictDataException.class,
                () -> commentService.update(eventVisitor.getId(), savedComment.getId(), updateComment));
        Assertions.assertTrue(exception.getMessage().contains(("Comment available to edit only 24 after publishing")));
    }

    @Test
    void deleteShouldDeleteCommentCorrectly() throws Exception {
        CommentDto resultComment = commentService.create(eventVisitor.getId(), savedEvent.getId(), newComment);

        commentService.delete(resultComment.getUser(), resultComment.getId());

        Optional<Comment> deletedComment = commentRepository.findById(resultComment.getId());

        Assertions.assertTrue(deletedComment.isEmpty());
    }

    @Test
    void deleteShouldThrowNotFoundWhenCommentIdIncorrect() throws Exception {
        CommentDto resultComment = commentService.create(eventVisitor.getId(), savedEvent.getId(), newComment);

        NotFoundException exception = Assertions.assertThrows(NotFoundException.class,
                () -> commentService.delete(resultComment.getUser(), 1000L));
        Assertions.assertTrue(exception.getMessage().contains(("Couldn't find comment by id=" + 1000L)));
    }

    @Test
    void deleteShouldThrowNotFoundWhenAuthorIdIncorrect() throws Exception {
        CommentDto resultComment = commentService.create(eventVisitor.getId(), savedEvent.getId(), newComment);

        ConflictDataException exception = Assertions.assertThrows(ConflictDataException.class,
                () -> commentService.delete(1000L, resultComment.getId()));
        Assertions.assertTrue(exception.getMessage().contains("Edit comment can only author"));
    }

    @Test
    void deleteShouldThrowNotFoundWhenCommentNotAvailableToDelete() throws Exception {
        CommentDto resultComment = commentService.create(eventVisitor.getId(), savedEvent.getId(), newComment);
        Comment comment = new Comment(resultComment.getId(), newComment.getText(),
                savedEvent, eventVisitor, LocalDateTime.now().minusDays(2));
        commentRepository.save(comment);

        ConflictDataException exception = Assertions.assertThrows(ConflictDataException.class,
                () -> commentService.delete(eventVisitor.getId(), resultComment.getId()));
        Assertions.assertTrue(exception.getMessage().contains("Comment available to edit only 24 after publishing"));
    }

    @Test
    void deleteByAdminShouldDeleteCommentCorrectly() throws Exception {
        CommentDto resultComment = commentService.create(eventVisitor.getId(), savedEvent.getId(), newComment);

        commentService.deleteByAdmin(resultComment.getId());

        Optional<Comment> deletedComment = commentRepository.findById(resultComment.getId());

        Assertions.assertTrue(deletedComment.isEmpty());
    }

    @Test
    void deleteByAdminShouldThrowNotFoundWhenCommentIdIncorrect() {
        NotFoundException exception = Assertions.assertThrows(NotFoundException.class,
                () -> commentService.deleteByAdmin(1000L));
        Assertions.assertTrue(exception.getMessage().contains("Couldn't find comment by id=1000"));
    }

    @Test
    void getByUserIdAndIdShouldReturnCommentCorrectly() throws Exception {
        CommentDto savedComment = commentService.create(eventVisitor.getId(), savedEvent.getId(), newComment);

        CommentDto resultComment = commentService.getByUserIdAndId(eventVisitor.getId(), savedComment.getId());

        Assertions.assertEquals(savedComment, resultComment);
    }

    @Test
    void getByUserIdAndIdShouldThrowNotFoundWhenCommentBelongsAnotherUser() throws Exception {
        CommentDto savedComment = commentService.create(eventVisitor.getId(), savedEvent.getId(), newComment);

        NotFoundException exception = Assertions.assertThrows(NotFoundException.class,
                () -> commentService.getByUserIdAndId(eventInitiator.getId(), savedComment.getId()));
        Assertions.assertTrue(exception.getMessage().contains("Couldn't find comment by id="
                + savedComment.getId() + " for userId=" + eventInitiator.getId()));
    }

    @Test
    void getByIdShouldReturnCommentCorrectly() throws Exception {
        CommentDto savedComment = commentService.create(eventVisitor.getId(), savedEvent.getId(), newComment);

        CommentDto resultComment = commentService.getById(savedComment.getId());

        Assertions.assertEquals(savedComment, resultComment);
    }

    @Test
    void getByIdShouldThrowNotFoundWhenCommentIdIncorrect() {
        NotFoundException exception = Assertions.assertThrows(NotFoundException.class,
                () -> commentService.getById(1000L));
        Assertions.assertTrue(exception.getMessage().contains("Couldn't find comment by id=1000"));
    }

    @Test
    void getAllByEventShouldReturnPageWithDataCorrectly() throws Exception {
        CommentDto savedComment = commentService.create(eventVisitor.getId(), savedEvent.getId(), newComment);
        Pageable pageable = PageRequest.of(0 / 10, 10);

        Page<CommentDto> page = commentService.getAllByEvent(savedEvent.getId(), pageable);
        List<CommentDto> list = page.getContent();
        CommentDto resultCommentDto = list.getFirst();

        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals(savedComment.getId(), resultCommentDto.getId());
        Assertions.assertEquals(savedComment.getEvent(), resultCommentDto.getEvent());
        Assertions.assertEquals(savedComment.getUser(), resultCommentDto.getUser());
        Assertions.assertEquals(savedComment.getText(), resultCommentDto.getText());
        Assertions.assertEquals(savedComment.getCreatedOn(), resultCommentDto.getCreatedOn());
    }

    @Test
    void getAllByEventShouldReturnEmptyPageCorrectly() {
        Pageable pageable = PageRequest.of(0 / 10, 10);

        Page<CommentDto> page = commentService.getAllByEvent(savedEvent.getId(), pageable);
        List<CommentDto> list = page.getContent();

        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    void getAllByEventShouldThrowNotFoundWhenEventIdIncorrect() {
        Pageable pageable = PageRequest.of(0 / 10, 10);

        NotFoundException exception = Assertions.assertThrows(NotFoundException.class,
                () -> commentService.getAllByEvent(1000L, pageable));
        Assertions.assertTrue(exception.getMessage().contains("Couldn't find event by id=1000"));
    }

    @Test
    void getAllShouldReturnPageWithDataCorrectly() throws Exception {
        CommentDto savedComment = commentService.create(eventVisitor.getId(), savedEvent.getId(), newComment);
        Pageable pageable = PageRequest.of(0 / 10, 10);

        Page<CommentDto> page = commentService.getAll(eventVisitor.getId(), pageable);
        List<CommentDto> list = page.getContent();
        CommentDto resultCommentDto = list.getFirst();

        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals(savedComment.getId(), resultCommentDto.getId());
        Assertions.assertEquals(savedComment.getEvent(), resultCommentDto.getEvent());
        Assertions.assertEquals(savedComment.getUser(), resultCommentDto.getUser());
        Assertions.assertEquals(savedComment.getText(), resultCommentDto.getText());
        Assertions.assertEquals(savedComment.getCreatedOn(), resultCommentDto.getCreatedOn());
    }

    @Test
    void getAllShouldReturnEmptyPageCorrectly() throws Exception {
        Pageable pageable = PageRequest.of(0 / 10, 10);

        Page<CommentDto> page = commentService.getAll(eventVisitor.getId(), pageable);
        List<CommentDto> list = page.getContent();

        Assertions.assertTrue(list.isEmpty());
    }
}
