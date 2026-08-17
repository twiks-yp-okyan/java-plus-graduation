package ru.yandex.practicum.comment.dto;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class CommentDto {
    private Long id;
    private String text;
    private Long event;
    private Long user;
    private String createdOn;
}