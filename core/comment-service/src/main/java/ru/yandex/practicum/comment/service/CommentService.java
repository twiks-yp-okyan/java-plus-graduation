package ru.yandex.practicum.comment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.comment.dto.CommentDto;
import ru.yandex.practicum.comment.dto.NewComment;

public interface CommentService {
    CommentDto create(Long userId, Long eventId, NewComment newComment);

    CommentDto update(Long userId, Long commentId, NewComment newComment);

    void delete(Long userId, Long commentId);

    void deleteByAdmin(Long commentId);

    CommentDto getByUserIdAndId(Long userId, Long commentId);

    CommentDto getById(Long commentId);

    Page<CommentDto> getAllByEvent(Long eventId, Pageable pageable);

    Page<CommentDto> getAll(Long userId, Pageable pageable);
}
