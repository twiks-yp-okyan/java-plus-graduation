package ru.yandex.practicum.ewm.collector.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserActionEvent {
    private Integer userId;
    private Integer eventId;
    private UserActionType actionType;
    private Instant timestamp = Instant.now();
}
