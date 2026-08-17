package ru.practicum.explorewithme.dto.compilation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewCompilationDto {
    @Builder.Default
    private Set<Long> events = new LinkedHashSet<>();

    @Builder.Default
    private Boolean pinned = false;

    @NotBlank
    @Size(max = 50)
    private String title;
}
