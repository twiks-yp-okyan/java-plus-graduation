package ru.practicum.explorewithme.controller.comment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.dto.comment.CommentDto;
import ru.practicum.explorewithme.dto.comment.NewComment;
import ru.practicum.explorewithme.service.comment.CommentService;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/comments")
public class CommentPrivateController {
    private final CommentService commentService;

    @PostMapping("/events/{eventId}")
    public ResponseEntity<CommentDto> create(@PathVariable Long userId,
                                             @PathVariable Long eventId,
                                             @Valid @RequestBody NewComment newComment) {
        CommentDto commentDto = commentService.create(userId, eventId, newComment);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(commentDto);
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentDto> update(@PathVariable Long userId,
                                             @PathVariable Long commentId,
                                             @Valid @RequestBody NewComment newComment) {
        CommentDto commentDto = commentService.update(userId, commentId, newComment);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(commentDto);
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommentDto> getById(@PathVariable Long userId,
                                              @PathVariable Long commentId) {
        CommentDto commentDto = commentService.getByUserIdAndId(userId, commentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(commentDto);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long userId,
                                       @PathVariable Long commentId) {
        commentService.delete(userId, commentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .build();
    }

    @GetMapping
    public ResponseEntity<List<CommentDto>> getAll(@PathVariable Long userId,
                                                   @RequestParam(defaultValue = "0") @Min(0) Integer from,
                                                   @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<CommentDto> comments = commentService.getAll(userId, pageable).getContent();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(comments);
    }
}
