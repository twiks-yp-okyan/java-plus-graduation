package ru.yandex.practicum.request.dto.event;

import lombok.*;
import ru.yandex.practicum.request.dto.category.CategoryDto;
import ru.yandex.practicum.request.dto.location.LocationDto;
import ru.yandex.practicum.request.dto.user.UserShortDto;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class EventFullDto {
    private Long id;
    private String annotation;
    private CategoryDto category;
    private Integer confirmedRequests;
    private String createdOn;
    private String description;
    private String eventDate;
    private UserShortDto initiator;
    private LocationDto location;
    private Boolean paid;
    private Integer participantLimit;
    private String publishedOn;
    private Boolean requestModeration;
    private String state;
    private String title;
    private Long views;
}
