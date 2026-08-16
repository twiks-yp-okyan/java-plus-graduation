package ru.yandex.practicum.comment.model;

import lombok.experimental.UtilityClass;
import ru.yandex.practicum.comment.dto.CommentDto;
import ru.yandex.practicum.comment.dto.NewComment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class CommentMapper {
    private final DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Comment toComment(NewComment newComment, Long userId, Long eventId) {
        return Comment.builder()
                .text(newComment.getText())
                .createdOn(LocalDateTime.now())
                .userId(userId)
                .eventId(eventId)
                .build();
    }

    public static void updateComment(Comment comment, NewComment newComment) {
        comment.setText(newComment.getText());
    }

    public CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .user(comment.getUserId())
                .event(comment.getEventId())
                .text(comment.getText())
                .createdOn(comment.getCreatedOn().format(customFormatter))
                .build();
    }
}
