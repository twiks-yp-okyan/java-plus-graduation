package ru.yandex.practicum.comment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.comment.service.CommentService;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/admin/comments")
public class CommentAdminController {
    private final CommentService commentService;

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long commentId) {
        commentService.deleteByAdmin(commentId);
        return ResponseEntity.ok().build();
    }
}
