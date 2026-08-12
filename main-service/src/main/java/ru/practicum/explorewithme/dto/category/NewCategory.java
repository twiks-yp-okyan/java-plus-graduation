package ru.practicum.explorewithme.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewCategory {
    @NotBlank(message = "Name must not be null, empty or consist only of whitespace")
    @Size(min = 1, max = 50, message = "Name length must be between 1 and 50 characters")
    private String name;
}