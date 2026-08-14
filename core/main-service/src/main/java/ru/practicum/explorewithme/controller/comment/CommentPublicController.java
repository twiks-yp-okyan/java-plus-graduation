package ru.practicum.explorewithme.controller.comment;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explorewithme.dto.comment.CommentDto;
import ru.practicum.explorewithme.service.comment.CommentService;

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
