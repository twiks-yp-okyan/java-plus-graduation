package ru.practicum.explorewithme.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.explorewithme.dto.compilation.CompilationDto;
import ru.practicum.explorewithme.model.compilation.Compilation;

import java.util.Set;

@UtilityClass
public class CompilationMapper {
    public CompilationDto toCompilationDto(Compilation compilation, Set<ru.practicum.explorewithme.dto.event.EventShortDto> events) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .events(events)
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }
}
