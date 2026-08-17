package ru.yandex.practicum.comment.dto.event;

import lombok.*;
import ru.yandex.practicum.comment.dto.category.CategoryDto;
import ru.yandex.practicum.comment.dto.user.UserShortDto;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class EventShortDto {
    private Long id;
    private String annotation;
    private CategoryDto category;
    private Integer confirmedRequests;
    private String eventDate;
    private UserShortDto initiator;
    private Boolean paid;
    private String title;
    private Long views;
}
