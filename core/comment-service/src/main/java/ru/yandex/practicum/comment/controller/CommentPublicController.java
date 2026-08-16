package ru.yandex.practicum.comment.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.comment.dto.CommentDto;
import ru.yandex.practicum.comment.service.CommentService;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentPublicController {
    private final CommentService commentService;

    @GetMapping("/{commentId}")
    public ResponseEntity<CommentDto> getById(@PathVariable Long commentId) {
        CommentDto commentDto = commentService.getById(commentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(commentDto);
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<List<CommentDto>> getAllByEvent(@PathVariable Long eventId,
                                                          @RequestParam(defaultValue = "0") @Min(0) Integer from,
                                                          @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<CommentDto> comments = commentService.getAllByEvent(eventId, pageable).getContent();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(comments);
    }
}
