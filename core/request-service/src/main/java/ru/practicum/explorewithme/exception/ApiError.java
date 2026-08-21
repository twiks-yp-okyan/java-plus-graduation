package ru.practicum.explorewithme.exception;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiError {
    private String status;
    private String reason;
    private String message;
    private String timestamp;
}
