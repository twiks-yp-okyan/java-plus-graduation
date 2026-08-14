package ru.practicum.explorewithme.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.explorewithme.dto.comment.CommentDto;
import ru.practicum.explorewithme.dto.comment.NewComment;
import ru.practicum.explorewithme.model.comment.Comment;
import ru.practicum.explorewithme.model.event.Event;
import ru.practicum.explorewithme.model.user.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class CommentMapper {
    private final DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Comment toComment(NewComment newComment, User user, Event event) {
        return Comment.builder()
                .text(newComment.getText())
                .createdOn(LocalDateTime.now())
                .user(user)
                .event(event)
                .build();
    }

    public static Comment toComment(Comment comment, NewComment newComment) {
        return Comment.builder()
                .text(newComment.getText())
                .createdOn(comment.getCreatedOn())
                .user(comment.getUser())
                .event(comment.getEvent())
                .id(comment.getId())
                .build();
    }

    public CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .user(comment.getUser().getId())
                .event(comment.getEvent().getId())
                .text(comment.getText())
                .createdOn(comment.getCreatedOn().format(customFormatter))
                .build();
    }
}
