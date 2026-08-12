package ru.practicum.explorewithme.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class NewComment {
    @NotBlank
    @Size(min = 20, max = 2000, message = "Text message must be not less than 20 and no more than 2000 symbols")
    private String text;
}
