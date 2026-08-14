package ru.practicum.explorewithme.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.explorewithme.dto.comment.CommentDto;
import ru.practicum.explorewithme.dto.comment.NewComment;
import ru.practicum.explorewithme.model.comment.Comment;
import ru.practicum.explorewithme.model.event.Event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class CommentMapper {
    private final DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Comment toComment(NewComment newComment, Long userId, Event event) {
        return Comment.builder()
                .text(newComment.getText())
                .createdOn(LocalDateTime.now())
                .userId(userId)
                .event(event)
                .build();
    }

    public static void updateComment(Comment comment, NewComment newComment) {
        comment.setText(newComment.getText());
    }

    public CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .user(comment.getUserId())
                .event(comment.getEvent().getId())
                .text(comment.getText())
                .createdOn(comment.getCreatedOn().format(customFormatter))
                .build();
    }
}
