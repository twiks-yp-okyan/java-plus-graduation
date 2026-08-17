package ru.yandex.practicum.user.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(ConflictDataException.class)
    public ResponseEntity<ApiError> handleConflictData(ConflictDataException exception) {
        log.error("409 {}", exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.builder()
                .status(HttpStatus.CONFLICT.toString())
                .reason("409 CONFLICT")
                .message(exception.getMessage())
                .timestamp(LocalDateTime.now().format(dateTimeFormatter))
                .build());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException exception) {
        log.error("404 {}", exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.builder()
                .status(HttpStatus.NOT_FOUND.toString())
                .reason("404 NOT_FOUND")
                .message(exception.getMessage())
                .timestamp(LocalDateTime.now().format(dateTimeFormatter))
                .build());
    }
}
