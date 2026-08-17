package ru.yandex.practicum.request.dto;

public record ParticipationRequestDto(Long id,
                                      String created,
                                      Long event,
                                      Long requester,
                                      String status) {
}
