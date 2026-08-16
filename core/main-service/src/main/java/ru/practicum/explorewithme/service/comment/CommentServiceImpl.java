package ru.practicum.explorewithme.service.comment;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.dto.comment.CommentDto;
import ru.practicum.explorewithme.dto.comment.NewComment;
import ru.practicum.explorewithme.dto.user.UserDto;
import ru.practicum.explorewithme.exception.ConflictDataException;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.feign.RequestClient;
import ru.practicum.explorewithme.feign.UserClient;
import ru.practicum.explorewithme.mapper.CommentMapper;
import ru.practicum.explorewithme.model.comment.Comment;
import ru.practicum.explorewithme.model.event.Event;
import ru.practicum.explorewithme.repository.CommentRepository;
import ru.practicum.explorewithme.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private static final int AVAILABLE_TIME_TO_COMMENT_EDIT = 24;
    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserClient userClient;
    private final RequestClient requestClient;

    @Transactional
    @Override
    public CommentDto create(Long userId, Long eventId, NewComment newComment) {
        log.info("Try to create comment userId={}, eventId={}, newComment={}", userId, eventId, newComment);
        UserDto user = getUserById(userId);
        Event event = getEventFromDB(eventId);

        checkUserNotEventOwner(event, user.getId());
        checkNoCommentFromUserToEvent(userId, eventId);
        checkRequestWasConfirmed(event.getId(), user.getId());

        Comment comment = CommentMapper.toComment(newComment, user.getId(), event);
        Comment saveComment = commentRepository.save(comment);
        log.info("Comment was saved");
        return CommentMapper.toDto(saveComment);
    }

    @Transactional
    @Override
    public CommentDto update(Long userId, Long commentId, NewComment newComment) {
        log.info("Try to update comment userId={}, commentId={}, newComment={}", userId, commentId, newComment);
        Comment comment = getCommentFromDB(commentId);
        checkUserIsCommentAuthor(userId, comment);
        checkCommentAvailableToEdit(comment);
        CommentMapper.updateComment(comment, newComment);
        Comment savedComment = commentRepository.saveAndFlush(comment);
        log.info("Comment was updated");
        return CommentMapper.toDto(savedComment);
    }

    @Transactional
    @Override
    public void delete(Long userId, Long commentId) {
        log.info("Try to delete comment commentId={}, userId={}", commentId, userId);
        Comment comment = getCommentFromDB(commentId);
        checkUserIsCommentAuthor(userId, comment);
        checkCommentAvailableToEdit(comment);
        commentRepository.delete(comment);
        log.info("Comment was deleted");
    }

    @Override
    @Transactional
    public void deleteByAdmin(Long commentId) {
        log.info("Try to delete comment by admin commentId={}", commentId);
        Comment comment = getCommentFromDB(commentId);
        commentRepository.delete(comment);
        log.info("Comment was deleted by admin");
    }

    @Override
    public CommentDto getByUserIdAndId(Long userId, Long commentId) {
        log.info("Try to get comment by userId={}, commentId={}", userId, commentId);
        UserDto user = getUserById(userId);
        Comment comment = getCommentFromDB(commentId);
        if (!comment.getUserId().equals(user.getId())) {
            throw new NotFoundException("Couldn't find comment by id=" + commentId + " for userId=" + userId);
        }
        return CommentMapper.toDto(comment);
    }

    @Override
    public CommentDto getById(Long commentId) {
        log.info("Try to get comment by id={}", commentId);
        Comment comment = getCommentFromDB(commentId);
        return CommentMapper.toDto(comment);
    }

    @Override
    public Page<CommentDto> getAllByEvent(Long eventId, Pageable pageable) {
        log.info("Try to get all comments by eventId={}, pageable={}", eventId, pageable);
        getEventFromDB(eventId);
        Page<Comment> comments = commentRepository.findAllByEventId(eventId, pageable);
        log.info("Get all comments by eventId={}", eventId);
        return comments.map(CommentMapper::toDto);
    }

    @Override
    public Page<CommentDto> getAll(Long userId, Pageable pageable) {
        log.info("Try to get all comments userId={}, pageable={}", userId, pageable);
        UserDto user = getUserById(userId);
        Page<Comment> comments = commentRepository.findAllByUserId(user.getId(), pageable);
        log.info("Get all comments");
        return comments.map(CommentMapper::toDto);
    }

    private void checkCommentAvailableToEdit(Comment comment) {
        if (comment.getCreatedOn().plusHours(AVAILABLE_TIME_TO_COMMENT_EDIT).isBefore(LocalDateTime.now())) {
            throw new ConflictDataException("Comment available to edit only 24 after publishing");
        }
    }

    private void checkUserIsCommentAuthor(Long userId, Comment comment) {
        if (!comment.getUserId().equals(userId)) {
            throw new ConflictDataException("Edit comment can only author");
        }
    }

    private Comment getCommentFromDB(Long commentId) {
        Optional<Comment> commentOptional = commentRepository.findById(commentId);
        return commentOptional.orElseThrow(
                () -> new NotFoundException("Couldn't find comment by id=" + commentId));
    }

    private void checkNoCommentFromUserToEvent(Long userId, Long eventId) {
        Optional<Comment> commentOptional = commentRepository.findByUserIdAndEventId(userId, eventId);
        if (commentOptional.isPresent()) {
            throw new ConflictDataException("Comment has already in DB from userId=" + userId
                    + " to eventId=" + eventId);
        }
    }

    private void checkRequestWasConfirmed(Long eventId, Long userId) {
        if (!requestClient.checkUserRequestConfirmation(eventId, userId)) {
            throw new ConflictDataException("Create comment can only event's visitor");
        }
    }

    private void checkUserNotEventOwner(Event event, Long userId) {
        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictDataException("User cant create comment for his event");
        }
    }

    private Event getEventFromDB(Long eventId) {
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        return eventOptional.orElseThrow(() -> new NotFoundException("Couldn't find event by id=" + eventId));
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
