package ru.practicum.explorewithme.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.explorewithme.model.comment.Comment;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByUserIdAndEventId(Long userId, Long eventId);

    Page<Comment> findAllByEventId(Long eventId, Pageable pageable);

    Page<Comment> findAllByUserId(Long userId, Pageable pageable);
}
