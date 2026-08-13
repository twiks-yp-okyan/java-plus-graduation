package ru.practicum.explorewithme.dto.request;

public record ParticipationRequestDto(Long id,
                                      String created,
                                      Long event,
                                      Long requester,
                                      String status) {
}
