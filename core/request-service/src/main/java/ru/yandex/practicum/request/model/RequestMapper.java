package ru.yandex.practicum.request.model;

import lombok.experimental.UtilityClass;
import ru.yandex.practicum.request.dto.ParticipationRequestDto;

import java.time.format.DateTimeFormatter;

@UtilityClass
public class RequestMapper {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ParticipationRequestDto toParticipationRequestDto(Request request) {
        return new ParticipationRequestDto(
                request.getId(),
                request.getCreated().format(formatter),
                request.getEventId(),
                request.getRequesterId(),
                request.getStatus().name());
    }
}
